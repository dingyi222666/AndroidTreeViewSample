package com.example.treeviewsample

 class TreeNode2(
    val id: String,
    val name: String,
    val children: MutableList<TreeNode2> = mutableListOf(),
    var isExpanded: Boolean = false,
    var level: Int = 0, //
    var isSwitchOn: Boolean = false,
    //var parent: TreeNode2? = null
)