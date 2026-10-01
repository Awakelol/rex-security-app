package io.github.awakelol.rex.watch

import io.github.awakelol.rex.data.PendingApp

/**
 * Everything Rex does in response to a new or pending app goes through here,
 * so a stronger implementation (e.g. one that can suspend apps) can be swapped in later.
 */
interface Enforcer {
    suspend fun onNewPackage(app: PendingApp)
    suspend fun onPendingPackageStillPresent(app: PendingApp)
    suspend fun onPendingPackageRemoved(app: PendingApp)
    suspend fun onPackageApproved(app: PendingApp)
}
