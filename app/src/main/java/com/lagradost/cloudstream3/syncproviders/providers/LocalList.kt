package com.lagradost.cloudstream3.syncproviders.providers

import androidx.fragment.app.FragmentActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.syncproviders.AuthAPI
import com.lagradost.cloudstream3.syncproviders.SyncAPI
import com.lagradost.cloudstream3.syncproviders.SyncIdName
import com.lagradost.cloudstream3.ui.WatchType
import com.lagradost.cloudstream3.ui.library.ListSorting
import com.lagradost.cloudstream3.utils.txt
import com.lagradost.cloudstream3.ui.settings.Globals.TV
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.utils.Coroutines.ioWork
import com.lagradost.cloudstream3.utils.DataStoreHelper.getAllFavorites
import com.lagradost.cloudstream3.utils.DataStoreHelper.getAllSubscriptions
import com.lagradost.cloudstream3.utils.DataStoreHelper.getAllWatchStateIds
import com.lagradost.cloudstream3.utils.DataStoreHelper.getBookmarkedData
import com.lagradost.cloudstream3.utils.DataStoreHelper.getResultWatchState

class LocalList : SyncAPI {
    override val name = "Local"
    override val icon: Int = R.drawable.ic_baseline_storage_24
    override val requiresLogin = false
    override val supportDeviceAuth = false
    override val createAccountUrl: Nothing? = null
    override val idPrefix = "local"
    override var requireLibraryRefresh = true

    override fun loginInfo(): AuthAPI.LoginInfo {
        return AuthAPI.LoginInfo(
            null,
            null,
            0
        )
    }

    override fun logOut() {

    }

    override val key: String = ""
    override val redirectUrl = ""
    override suspend fun handleRedirect(url: String): Boolean {
        return true
    }

    override fun authenticate(activity: FragmentActivity?) {
    }

    override val mainUrl = ""
    override val syncIdName = SyncIdName.LocalList
    override suspend fun score(id: String, status: SyncAPI.AbstractSyncStatus): Boolean {
        return true
    }

    override suspend fun getStatus(id: String): SyncAPI.AbstractSyncStatus? {
        return null
    }

    override suspend fun getResult(id: String): SyncAPI.SyncResult? {
        return null
    }

    override suspend fun search(name: String): List<SyncAPI.SyncSearchResult>? {
        return null
    }

    override suspend fun getPersonalLibrary(): SyncAPI.LibraryMetadata? {
        val watchStatusIds = ioWork {
            getAllWatchStateIds()?.map { id ->
                Pair(id, getResultWatchState(id))
            }
        }?.distinctBy { it.first } ?: return null

        val list = ioWork {
            val isTrueTv = isLayout(TV)

            val baseMap = WatchType.entries.filter { it != WatchType.NONE }.associate {
                // None is not something to display
                it.stringRes to emptyList<SyncAPI.LibraryItem>()
            } + mapOf(
                R.string.favorites_list_name to emptyList()
            ) + if (!isTrueTv) {
                mapOf(
                    R.string.subscription_list_name to emptyList()
                )
            } else {
                emptyMap()
            }

            val watchStatusMap = watchStatusIds.groupBy { it.second.stringRes }.mapValues { group ->
                group.value.mapNotNull {
                    getBookmarkedData(it.first)?.toLibraryItem(it.first.toString())
                }
            }

            val favoritesMap = mapOf(R.string.favorites_list_name to getAllFavorites().mapNotNull {
                it.toLibraryItem()
            })

            // Don't show subscriptions on TV
            val result = if (isTrueTv) {
                baseMap + watchStatusMap + favoritesMap
            } else {
                val subscriptionsMap = mapOf(R.string.subscription_list_name to getAllSubscriptions().mapNotNull {
                    it.toLibraryItem()
                })

                baseMap + watchStatusMap + subscriptionsMap + favoritesMap
            }

            result
        }

        return SyncAPI.LibraryMetadata(
            list.map { SyncAPI.LibraryList(txt(it.key), it.value) },
            setOf(
                ListSorting.AlphabeticalA,
                ListSorting.AlphabeticalZ,
                ListSorting.UpdatedNew,
                ListSorting.UpdatedOld,
                ListSorting.ReleaseDateNew,
                ListSorting.ReleaseDateOld,
//                ListSorting.RatingHigh,
//                ListSorting.RatingLow,

            )
        )
    }

    override fun getIdFromUrl(url: String): String {
        return url
    }
}