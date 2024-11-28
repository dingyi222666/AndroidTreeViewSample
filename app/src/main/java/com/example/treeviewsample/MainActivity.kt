package com.example.treeviewsample

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Space
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.example.treeviewsample.databinding.ActivityMainBinding
import com.example.treeviewsample.databinding.ItemDirBinding
import com.example.treeviewsample.databinding.ItemFileBinding
import io.github.dingyi222666.view.treeview.AbstractTree
import io.github.dingyi222666.view.treeview.Tree
import io.github.dingyi222666.view.treeview.TreeNode
import io.github.dingyi222666.view.treeview.TreeNodeEventListener
import io.github.dingyi222666.view.treeview.TreeNodeGenerator
import io.github.dingyi222666.view.treeview.TreeView
import io.github.dingyi222666.view.treeview.TreeViewBinder
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val source = DataSource()
        val trees = createTree(source)
        val binder = ViewBinder()

        (binding.treeview as TreeView<TreeNode2>).apply {
            bindCoroutineScope(lifecycleScope)
            tree = trees
            this.binder = binder
            selectionMode = TreeView.SelectionMode.MULTIPLE_WITH_CHILDREN
            nodeEventListener = binder

            binder.onSelected { node, selected ->
                // selected from data (update data)
                source.select(node.data!!, selected)

                Log.d("MainActivity", "select ${node.data!!.name} $selected")

                // refresh the tree
                lifecycleScope.launch {
                    selectionMode = if (selected) {
                        TreeView.SelectionMode.MULTIPLE_WITH_CHILDREN
                    } else {
                        TreeView.SelectionMode.MULTIPLE
                    }
                    binding.treeview.selectNode(node, selected)
                    selectionMode = TreeView.SelectionMode.MULTIPLE_WITH_CHILDREN


                    val selectedNode = tree.getSelectedNodes()

                    Toast.makeText(
                        this@MainActivity,
                        "Selected ${selectedNode.joinToString { it.name.toString() }}.",
                        Toast.LENGTH_LONG
                    ).apply {
                        show()
                    }
                }
            }
        }

        lifecycleScope.launch {
            binding.treeview.refresh()
        }
    }


    private fun createTree(source: DataSource): Tree<TreeNode2> {
        return Tree.createTree<TreeNode2>().apply {
            generator = ItemTreeNodeGenerator(source)
            initTree()
        }
    }


    inner class ViewBinder : TreeViewBinder<TreeNode2>(),
        TreeNodeEventListener<TreeNode2> {

        private var selectedEventListener: (node: TreeNode<TreeNode2>, selected: Boolean) -> Unit =
            { _, _ -> }

        override fun createView(parent: ViewGroup, viewType: Int): View {
            val layoutInflater = LayoutInflater.from(parent.context)
            return if (viewType == 1) {
                ItemDirBinding.inflate(layoutInflater, parent, false).root
            } else {
                ItemFileBinding.inflate(layoutInflater, parent, false).root
            }
        }

        override fun getItemViewType(node: TreeNode<TreeNode2>): Int {
            if (node.data!!.children.isNotEmpty()) {
                return 1
            }
            return 0
        }

        override fun bindView(
            holder: TreeView.ViewHolder,
            node: TreeNode<TreeNode2>,
            listener: TreeNodeEventListener<TreeNode2>
        ) {
            if (node.data!!.children.isNotEmpty()) {
                applyDir(holder, node)
            } else {
                applyFile(holder, node)
            }

            val itemView = holder.itemView.findViewById<Space>(R.id.space)


            (getCheckableView(node, holder)).apply {
                setOnCheckedChangeListener { _, selected ->
                    selectedEventListener.invoke(node, selected)
                }
            }

            itemView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                width = node.depth * 22
            }

        }

        override fun getCheckableView(
            node: TreeNode<TreeNode2>,
            holder: TreeView.ViewHolder
        ): Switch {

            return if (node.isChild) {
                ItemDirBinding.bind(holder.itemView).switchOn
            } else {
                ItemFileBinding.bind(holder.itemView).switchOn
            }
            //return  super.getCheckableView(node, holder)
        }

        private fun applyFile(holder: TreeView.ViewHolder, node: TreeNode<TreeNode2>) {
            val binding = ItemFileBinding.bind(holder.itemView)
            binding.tvName.text = node.data!!.name
        }

        private fun applyDir(holder: TreeView.ViewHolder, node: TreeNode<TreeNode2>) {
            val binding = ItemDirBinding.bind(holder.itemView)
            binding.tvName.text = node.data!!.name


            binding
                .ivArrow
                .animate()
                .rotation(if (node.expand) 180f else 0f)
                .setDuration(200)
                .start()
        }


        override fun onClick(node: TreeNode<TreeNode2>, holder: TreeView.ViewHolder) {
            Toast.makeText(this@MainActivity, "Clicked ${node.name}", Toast.LENGTH_LONG).show()
        }


        // call when the node has children
        override fun onToggle(
            node: TreeNode<TreeNode2>,
            isExpand: Boolean,
            holder: TreeView.ViewHolder
        ) {
            applyDir(holder, node)
        }

        fun onSelected(listener: (node: TreeNode<TreeNode2>, selected: Boolean) -> Unit) {
            selectedEventListener = listener
        }
    }
}


class DataSource {
    val root = TreeNode2("1", "Root 1")

    init {
        initData(root)
    }

    private fun initData(root: TreeNode2) {
        val child1 = TreeNode2("1.1", "Child 1")
        val child2 = TreeNode2("1.2", "Child 2")
        val grandChild1 = TreeNode2("1.2.1", "Grandchild 1")
        grandChild1.children.addAll(
            listOf(
                TreeNode2("1.2.1", "Grandchild 2"),
                TreeNode2("1.2.1", "Grandchild 3")
            )
        )
        child2.children.add(grandChild1)
        root.children.addAll(listOf(child1, child2))
    }

    fun select(node: TreeNode2, selected: Boolean = !node.selected) {
        node.selected = selected


        if (!selected) {
            // don't deselect children
            return
        }

        val currentStack = ArrayDeque<TreeNode2>()

        currentStack.add(node)

        while (currentStack.isNotEmpty()) {
            val current = currentStack.removeFirst()
            current.selected = true

            currentStack.addAll(current.children)
        }
    }


}

class ItemTreeNodeGenerator(
    private val rootSource: DataSource
) : TreeNodeGenerator<TreeNode2> {


    override fun createNode(
        parentNode: TreeNode<TreeNode2>,
        currentData: TreeNode2,
        tree: AbstractTree<TreeNode2>
    ): TreeNode<TreeNode2> {
        return TreeNode(
            data = currentData,
            depth = parentNode.depth + 1,
            name = currentData.name,
            id = tree.generateId(),
            hasChild = currentData.children.isNotEmpty(),
            // It should be taken from the Item
            isChild = currentData.children.isNotEmpty(),
            expand = false,
            selected = currentData.selected
        )
    }

    override fun createRootNode(): TreeNode<TreeNode2> {
        return TreeNode(
            data = rootSource.root,
            // Set to -1 to not show the root node
            depth = -1,
            name = rootSource.root.name,
            id = Tree.ROOT_NODE_ID,
            hasChild = true,
            isChild = true
        )
    }

    override suspend fun fetchChildData(targetNode: TreeNode<TreeNode2>): Set<TreeNode2> {
        return targetNode.requireData().children.toSet()
    }


}