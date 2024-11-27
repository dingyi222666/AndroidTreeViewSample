package com.example.treeviewsample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Checkable
import android.widget.CompoundButton
import android.widget.Space
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
    private lateinit var  binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val trees = createTree()

        (binding.treeview as TreeView<TreeNode2>).apply {
            bindCoroutineScope(lifecycleScope)
            this.tree = trees
            binder = ViewBinder()
            selectionMode=  TreeView.SelectionMode.MULTIPLE_WITH_CHILDREN
            nodeEventListener = binder as TreeNodeEventListener<TreeNode2>
        }

        lifecycleScope.launch {
            binding.treeview.refresh()
        }
    }


    private fun createTree(): Tree<TreeNode2> {

        val rootNode1 = TreeNode2("1", "Root 1")
        val child1 =
            TreeNode2("1.1", "Child 1")
        val child2 =
            TreeNode2("1.2", "Child 2")
        val grandChild1 =
            TreeNode2("1.2.1", "Grandchild 1")
        grandChild1.children.addAll(
            listOf(TreeNode2("1.2.1", "Grandchild 2"),TreeNode2("1.2.1", "Grandchild 3"))
        )
        child2.children.add(grandChild1)
        rootNode1.children.addAll(listOf(child1, child2))
        return Tree.createTree<TreeNode2>().apply {
            generator = ItemTreeNodeGenerator(rootNode1)
            initTree()
        }
    }




    private fun selectAllNode() {
        lifecycleScope.launch {
            binding.treeview.apply {
                // select node and it's children
                selectionMode = TreeView.SelectionMode.MULTIPLE_WITH_CHILDREN
                selectNode(binding.treeview.tree.rootNode, true)
                expandAll()
                selectionMode = TreeView.SelectionMode.MULTIPLE_WITH_CHILDREN
            }
        }
    }
}



class ViewBinder : TreeViewBinder<TreeNode2>(),
    TreeNodeEventListener<TreeNode2> {

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

//        (getCheckableView(node, holder) as Switch).apply {
//            isVisible = node.selected
//            isSelected = node.selected
//        }

        itemView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            width = node.depth * 22
        }

    }

    override fun getCheckableView(
        node: TreeNode<TreeNode2>,
        holder: TreeView.ViewHolder
    ): Checkable? {

        return if (node.isChild) {
            ItemDirBinding.bind(holder.itemView).switchOn
        } else {
            ItemFileBinding.bind(holder.itemView).switchOn
        }
        //return  super.getCheckableView(node, holder)
    }

    private fun applyFile(holder: TreeView.ViewHolder, node: TreeNode<TreeNode2>) {
        val binding = ItemFileBinding.bind(holder.itemView)
        binding.tvName.text = node.data!!.name.toString()
    }

    private fun applyDir(holder: TreeView.ViewHolder, node: TreeNode<TreeNode2>) {
        val binding = ItemDirBinding.bind(holder.itemView)
        binding.tvName.text = node.data!!.name.toString()
        binding.switchOn.isChecked =  node.data!!.isSwitchOn
        binding.switchOn.setOnCheckedChangeListener(object :
            CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(p0: CompoundButton?, p1: Boolean) {
//                 node.data!!.isSwitchOn = p1
//                 if (node.hasChild) {
//                     flattenTree(node.data!!.children, p1)
//                 }

            }
        })

        binding
            .ivArrow
            .animate()
            .rotation(if (node.expand) 180f else 0f)
            .setDuration(200)
            .start()
    }

    fun flattenTree(treeNodes: List<TreeNode2>, isSwitchOn: Boolean): List<TreeNode2> {
        val result = mutableListOf<TreeNode2>()
        for (node in treeNodes) {
            node.isSwitchOn = isSwitchOn
            result.add(node)
            if (node.children.isNotEmpty()) {
                val flattenedChildren = flattenTree(node.children, isSwitchOn)
                result.addAll(flattenedChildren)
            }
        }
        return result
    }

//     fun flattenTree(): List<TreeNode2> {
//         val result = mutableListOf<TreeNode2>()
//         for (node in treeNodes) {
//             node.isSwitchOn = isSwitchOn
//             result.add(node)
//             if (node.children.isNotEmpty()) {
//                 val flattenedChildren = flattenTree(node.children, isSwitchOn)
//                 result.addAll(flattenedChildren)
//             }
//         }
//         return result
//     }


    override fun onClick(node: TreeNode<TreeNode2>, holder: TreeView.ViewHolder) {
        if (node.data!!.children.isNotEmpty()) {
            applyDir(holder, node)
        } else {
            //Toast.makeText(this@MainActivity, "Clicked ${node.name}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onToggle(
        node: TreeNode<TreeNode2>,
        isExpand: Boolean,
        holder: TreeView.ViewHolder
    ) {
        applyDir(holder, node)
    }
}

class ItemTreeNodeGenerator(
    private val rootItem: TreeNode2
) : TreeNodeGenerator<TreeNode2> {
    suspend fun fetchNodeChildData(targetNode: TreeNode<TreeNode2>): Set<TreeNode2> {
        return targetNode.requireData().children.toSet()
    }

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
            expand = false
        )
    }

    override fun createRootNode(): TreeNode<TreeNode2> {
        return TreeNode(
            data = rootItem,
            // Set to -1 to not show the root node
            depth = -1,
            name = rootItem.name,
            id = Tree.ROOT_NODE_ID,
            hasChild = true,
            isChild = true
        )
    }

    override suspend fun fetchChildData(targetNode: TreeNode<TreeNode2>): Set<TreeNode2> {
        return targetNode.requireData().children.toSet()
    }


}