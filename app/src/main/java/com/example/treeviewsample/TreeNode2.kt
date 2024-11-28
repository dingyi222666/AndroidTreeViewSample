package com.example.treeviewsample

 class TreeNode2(
    val id: String,
    val name: String,
    val children: MutableList<TreeNode2> = mutableListOf(),
    var selected: Boolean = false
)