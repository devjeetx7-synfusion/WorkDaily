package com.synfusion.attendance.backup

import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

interface RefreshableViewModel {
    fun cancelActiveJobs()
    fun refreshOnRestore()
}

object DatabaseLifecycleCoordinator {
    private val viewModels = CopyOnWriteArrayList<WeakReference<RefreshableViewModel>>()

    fun registerViewModel(viewModel: RefreshableViewModel) {
        // Clean up garbage collected references and check if already registered
        val toRemove = mutableListOf<WeakReference<RefreshableViewModel>>()
        var exists = false
        for (ref in viewModels) {
            val vm = ref.get()
            if (vm == null) {
                toRemove.add(ref)
            } else if (vm === viewModel) {
                exists = true
            }
        }
        viewModels.removeAll(toRemove)
        if (!exists) {
            viewModels.add(WeakReference(viewModel))
        }
    }

    fun unregisterViewModel(viewModel: RefreshableViewModel) {
        val toRemove = mutableListOf<WeakReference<RefreshableViewModel>>()
        for (ref in viewModels) {
            val vm = ref.get()
            if (vm == null || vm === viewModel) {
                toRemove.add(ref)
            }
        }
        viewModels.removeAll(toRemove)
    }

    fun stopAndCancelEverything() {
        val toRemove = mutableListOf<WeakReference<RefreshableViewModel>>()
        for (ref in viewModels) {
            val vm = ref.get()
            if (vm != null) {
                try {
                    vm.cancelActiveJobs()
                } catch (e: Exception) {
                    // ignore
                }
            } else {
                toRemove.add(ref)
            }
        }
        viewModels.removeAll(toRemove)
    }

    fun refreshViewModels() {
        val toRemove = mutableListOf<WeakReference<RefreshableViewModel>>()
        for (ref in viewModels) {
            val vm = ref.get()
            if (vm != null) {
                try {
                    vm.refreshOnRestore()
                } catch (e: Exception) {
                    // ignore
                }
            } else {
                toRemove.add(ref)
            }
        }
        viewModels.removeAll(toRemove)
    }
}
