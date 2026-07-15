package eu.kanade.tachiyomi.ui.library

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.widget.AutofitRecyclerView

/**
 * Adapter for the library's category pager: one page per category, each hosting its own
 * recycler + [LibraryCategoryAdapter], so adjacent categories exist while swiping (like Mihon).
 */
class LibraryPagerAdapter(private val controller: LibraryController) :
    RecyclerView.Adapter<LibraryPagerAdapter.PageHolder>() {

    var categories: List<Category> = emptyList()
        private set

    private var filterQuery: String? = null
    private val attachedHolders = mutableSetOf<PageHolder>()

    @SuppressLint("NotifyDataSetChanged")
    fun setCategories(list: List<Category>) {
        val changed = categories.size != list.size ||
            categories.zip(list).any { (old, new) -> old.id != new.id || old.name != new.name }
        categories = list.toList()
        if (changed) {
            notifyDataSetChanged()
        } else {
            refreshPages()
        }
    }

    /** Re-binds the items of all live pages, e.g. after a library update or filter change. */
    fun refreshPages() {
        attachedHolders.toList().forEach { holder ->
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                categories.getOrNull(position)?.let { holder.bind(it) }
            }
        }
    }

    fun setFilter(query: String?) {
        if (filterQuery == query) return
        filterQuery = query
        refreshPages()
    }

    /** The category adapter of the page at [position], if that page is currently alive. */
    fun pageAdapterAt(position: Int): LibraryCategoryAdapter? =
        attachedHolders.firstOrNull { it.bindingAdapterPosition == position }?.categoryAdapter

    override fun getItemCount(): Int = categories.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val recycler = LayoutInflater.from(parent.context)
            .inflate(R.layout.library_grid_recycler, parent, false) as AutofitRecyclerView
        return PageHolder(recycler)
    }

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun onViewAttachedToWindow(holder: PageHolder) {
        attachedHolders.add(holder)
    }

    override fun onViewDetachedFromWindow(holder: PageHolder) {
        attachedHolders.remove(holder)
    }

    inner class PageHolder(val recycler: AutofitRecyclerView) : RecyclerView.ViewHolder(recycler) {
        val categoryAdapter = LibraryCategoryAdapter(controller)

        init {
            // Pages show only covers, Mihon style — the category name lives in the tab above
            categoryAdapter.setDisplayHeadersAtStartUp(false)
            recycler.adapter = categoryAdapter
            recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                        controller.onPageScrolled(recyclerView, bindingAdapterPosition)
                    }
                }
            })
        }

        fun bind(category: Category) {
            controller.configurePageRecycler(recycler, categoryAdapter)
            categoryAdapter.setFilter(filterQuery)
            categoryAdapter.setItems(controller.pageItems(category))
            categoryAdapter.isLongPressDragEnabled = controller.canDrag()
        }
    }
}
