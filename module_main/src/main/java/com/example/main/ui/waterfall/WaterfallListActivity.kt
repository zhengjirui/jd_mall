package com.example.main.ui.waterfall

import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.viewModel
import com.alibaba.android.arouter.facade.annotation.Route
import com.example.common.constants.RouterPaths
import com.example.common.base.BaseActivity
import com.example.main.R
import com.example.common.util.LoadingDialog
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.api.RefreshLayout
import kotlinx.android.synthetic.main.layout_header.*
import kotlinx.android.synthetic.main.layout_waterfall.*
import java.lang.reflect.Method

@Route(path = RouterPaths.WATERFALL_ACTIVITY)
class WaterfallListActivity: BaseActivity(R.layout.layout_waterfall), MavericksView {
    private lateinit var mCheckForGapMethod: Method
    private lateinit var mMarkItemDecorInsetsDirtyMethod: Method
    private lateinit var refreshLayout: RefreshLayout
    private lateinit var loadMoreLayout: RefreshLayout

    private val adapter: WaterfallListAdapter by lazy {
        WaterfallListAdapter(R.layout.layout_waterfall_item, arrayListOf())
    }
    private val staggeredGridLayoutManager: StaggeredGridLayoutManager by lazy {
        StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
    }
    private val loadingDialog: LoadingDialog by lazy { LoadingDialog(this) }
    private val viewModel: WaterfallViewModel by viewModel()

    override fun initView() {
        //返回
        leftText.setOnClickListener {
            finish()
        }

        //下拉刷新
        waterfallLayout.setRefreshHeader(ClassicsHeader(this))
        waterfallLayout.setOnRefreshListener { layout ->
            run {
                refreshLayout = layout
                viewModel.refresh(true)
            }
        }

        //瀑布列表
        staggeredGridLayoutManager.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE //解决加载下一页后重新排列的问题
        waterfall_recycler_view.layoutManager = staggeredGridLayoutManager
        waterfall_recycler_view.itemAnimator = null

        mCheckForGapMethod = StaggeredGridLayoutManager::class.java.getDeclaredMethod("checkForGaps")
        mMarkItemDecorInsetsDirtyMethod = RecyclerView::class.java.getDeclaredMethod("markItemDecorInsetsDirty")
        mCheckForGapMethod.isAccessible = true
        mMarkItemDecorInsetsDirtyMethod.isAccessible = true

        waterfall_recycler_view.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val result = mCheckForGapMethod.invoke(waterfall_recycler_view.layoutManager) as Boolean
                //如果发生了重新排序，刷新itemDecoration
                if(result) {
                    mMarkItemDecorInsetsDirtyMethod.invoke(waterfall_recycler_view)
                }
            }
        })
        adapter.setOnItemClickListener{_, view, position ->
            if (view.id == R.id.waterfallItemLayout) {
                Toast.makeText(this, adapter.data[position].name, Toast.LENGTH_SHORT).show()
            }
        }
        waterfall_recycler_view.adapter = adapter
        val space = resources.getDimension(R.dimen.waterfall_space)
        waterfall_recycler_view.addItemDecoration(SpacesItemDecoration(space.toInt()))

        //上拉加载更多
        waterfallLayout.setRefreshFooter(ClassicsFooter(this))
        waterfallLayout.setOnLoadMoreListener { layout ->
            run {
                loadMoreLayout = layout
                viewModel.loadMore()
            }
        }
        waterfallLayout.setEnableAutoLoadMore(true)

        addStateChangeListener()
    }

    override fun initData() {
        viewModel.refresh(false)
    }

    private fun addStateChangeListener() {
        viewModel.onEach {
            when (it.isLoading) {
                true -> loadingDialog.show()
                false -> {
                    loadingDialog.dismiss()

                    when (it.fetchType) {
                        ActionType.INIT -> {
                            adapter.setList(it.dataList)
                        }
                        ActionType.REFRESH -> {
                            adapter.setList(it.dataList)
                            refreshLayout.finishRefresh()
                        }
                        ActionType.LOADMORE -> {
                            adapter.addData(adapter.data.size, it.newList)
                            if (it.currentPage <= it.totalPage)
                                loadMoreLayout.finishLoadMore()
                            else
                                loadMoreLayout.finishLoadMoreWithNoMoreData()
                        }
                    }
                }
            }
        }
    }

    override fun invalidate() {
    }
}