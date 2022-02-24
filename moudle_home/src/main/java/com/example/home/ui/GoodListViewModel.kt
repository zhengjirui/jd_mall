package com.example.home.ui

import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.Success
import com.example.common.util.HttpUtil
import kotlinx.coroutines.Dispatchers

class GoodListViewModel(initialState: GoodListState): MavericksViewModel<GoodListState>(initialState) {
    private var apiService: ApiService = HttpUtil.instance.service(ApiService::class.java)

    fun refresh(isRefresh: Boolean) {
        setState { copy(currentPage = 1, isLoading = true) }
        withState {
            if (it.pageResponse is Loading) return@withState

            suspend {
                apiService.queryGoodListByPage(QueryGoodListParams(it.currentPage, it.pageSize))
            }.execute(Dispatchers.IO) { state ->
                copy(
                    currentPage = if (state is Success) currentPage.plus(1) else currentPage,
                    pageResponse = state,
                    isLoading = state !is Success,
                    fetchType = if (isRefresh) ActionType.REFRESH else ActionType.INIT,
                    dataList = (state()?.data?.dataList ?: it.dataList),
                    totalPage = (state()?.data?.totalPageCount ?: 0)
                )
            }
        }
    }

    fun loadMore() {
        setState { copy(isLoading = true) }
        withState {
            if (it.pageResponse is Loading) return@withState

            suspend {
                apiService.queryGoodListByPage(QueryGoodListParams(it.currentPage, it.pageSize))
            }.execute(Dispatchers.IO) { state ->
                copy(
                    pageResponse = state,
                    isLoading = state !is Success,
                    fetchType = ActionType.LOADMORE,
                    dataList = dataList + (state()?.data?.dataList ?: emptyList()),
                    newList = (state()?.data?.dataList ?: emptyList()),
                    currentPage = if (state is Success) currentPage.plus(1) else currentPage,
                    totalPage = (state()?.data?.totalPageCount ?: 0)
                )
            }
        }
    }
}