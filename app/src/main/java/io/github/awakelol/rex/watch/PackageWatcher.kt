package io.github.awakelol.rex.watch

import io.github.awakelol.rex.core.PackageDiff
import io.github.awakelol.rex.core.RiskLevel
import io.github.awakelol.rex.core.RiskScorer
import io.github.awakelol.rex.data.LogEntry
import io.github.awakelol.rex.data.LogKind
import io.github.awakelol.rex.data.PendingApp
import io.github.awakelol.rex.data.RexDatabase
import io.github.awakelol.rex.data.Settings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

/**
 * Keeps the approved and pending lists in sync with what's actually installed.
 * Calls come from the broadcast receiver, the periodic worker and the UI, sometimes
 * for the same package at once, so everything runs under one lock.
 */
class PackageWatcher(
    private val ownPackage: String,
    private val inspector: PackageInspector,
    private val settings: Settings,
    private val db: RexDatabase,
    private val enforcer: Enforcer,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val lock = Mutex()

    suspend fun ensureBaseline() = lock.withLock { baseline() }

    suspend fun onAdded(pkg: String) = lock.withLock {
        baseline()
        if (pkg in settings.approved.first() || db.pending().get(pkg) != null) return@withLock
        handleNew(pkg)
    }

    suspend fun onUpdated(pkg: String) = lock.withLock {
        baseline()
        if (pkg in settings.approved.first()) return@withLock
        val existing = db.pending().get(pkg)
        if (existing == null) {
            handleNew(pkg)
            return@withLock
        }
        // A pending app can update itself to ask for more, so score it again.
        val facts = inspector.facts(pkg) ?: return@withLock
        val report = RiskScorer.score(facts, approvedLabels())
        db.pending().upsert(existing.copy(label = facts.label, level = report.level, reasons = report.reasons))
        if (report.level > existing.level) {
            log(LogKind.INFO, pkg, "${facts.label} updated, risk now ${report.level}")
        }
    }

    suspend fun onRemoved(pkg: String) = lock.withLock {
        val pending = db.pending().get(pkg)
        if (pending != null) {
            removePending(pending)
        } else if (pkg in settings.approved.first()) {
            settings.unapprove(setOf(pkg))
            log(LogKind.REMOVED, pkg, "Approved app uninstalled: $pkg")
        }
    }

    suspend fun reconcile() = lock.withLock {
        baseline()
        val installed = inspector.installedPackages()
        if (ownPackage !in installed) return@withLock  // package list came back broken, don't trust it

        val pending = db.pending().all().associateBy { it.packageName }
        val diff = PackageDiff.between(installed, settings.approved.first(), pending.keys)
        diff.added.forEach { handleNew(it) }
        diff.removedPending.forEach { removePending(pending.getValue(it)) }
        if (diff.removedApproved.isNotEmpty()) {
            settings.unapprove(diff.removedApproved)
        }
    }

    suspend fun onUnlock() = lock.withLock {
        val time = now()
        for (app in db.pending().all()) {
            if (app.level == RiskLevel.LOW || time - app.lastWarnedAt < REWARN_INTERVAL) continue
            if (!inspector.isInstalled(app.packageName)) {
                removePending(app)
                continue
            }
            db.pending().markWarned(app.packageName, time)
            enforcer.onPendingPackageStillPresent(app)
        }
    }

    suspend fun approve(pkg: String) = lock.withLock {
        val app = db.pending().get(pkg) ?: return@withLock
        settings.approve(pkg)
        db.pending().delete(pkg)
        log(LogKind.APPROVED, pkg, "Approved ${app.label}")
        enforcer.onPackageApproved(app)
    }

    suspend fun rebaseline() = lock.withLock {
        val installed = inspector.installedPackages()
        val pending = db.pending().all()
        settings.saveBaseline(installed)
        db.pending().clear()
        pending.forEach { enforcer.onPackageApproved(it) }
        log(LogKind.APPROVED, null, "Re-baselined: approved all ${installed.size} installed apps")
    }

    private suspend fun baseline() {
        if (settings.baselineDone.first()) return
        val installed = inspector.installedPackages()
        settings.saveBaseline(installed)
        log(LogKind.INFO, null, "Saved ${installed.size} installed apps as the approved list")
    }

    private suspend fun handleNew(pkg: String) {
        // Preinstalled apps that arrive with a system update can't be malware from a download.
        if (inspector.isSystemApp(pkg)) {
            settings.approve(pkg)
            log(LogKind.APPROVED, pkg, "System app added by a phone update: $pkg")
            return
        }
        val facts = inspector.facts(pkg) ?: return
        val report = RiskScorer.score(facts, approvedLabels())
        val time = now()
        val app = PendingApp(
            packageName = pkg,
            label = facts.label,
            installer = facts.installer,
            level = report.level,
            reasons = report.reasons,
            detectedAt = time,
            lastWarnedAt = time,
        )
        db.pending().upsert(app)
        log(LogKind.INSTALLED, pkg, "${facts.label} installed (${report.level} risk)")
        enforcer.onNewPackage(app)
    }

    private suspend fun removePending(app: PendingApp) {
        db.pending().delete(app.packageName)
        log(LogKind.REMOVED, app.packageName, "${app.label} was removed")
        enforcer.onPendingPackageRemoved(app)
    }

    private suspend fun approvedLabels() = inspector.labels(settings.approved.first())

    private suspend fun log(kind: LogKind, pkg: String?, text: String) {
        db.log().insert(LogEntry(time = now(), kind = kind, packageName = pkg, text = text))
    }

    private companion object {
        val REWARN_INTERVAL = TimeUnit.HOURS.toMillis(1)
    }
}
