echo2 is now on github, I forked the extras project and submit this fix back to the project on 2018-01-17

The DND.js is an updated file for the Echo2Extras to solve a problem with Drag and Drop interaction
with echopointng Tree. When a tree node is collapsed and nodes from the branch contain drop targets
the targets may not be found in the DOM tree any longer. When the ExtrasDragSource.MessageProcessor
processes init messages in processInit() to add the drop targets a null element is referenced.

processInit -> dragSource.addDropTarget() -> ExtrasDragSource.getElementPosition() ->
new EchoCssUtil.Bounds()


Echo2Extras\src\webcontainer\java\nextapp\echo2\extras\webcontainer\resource\js\

addDropTarget: function(dropTargetId) {
    var dropTarget = document.getElementById(dropTargetId);
    // a drop target on a tree may disappear when the tree is collapsed,
    // if the target is null ignore it.
    if (dropTarget) {
    	this.dropTargetArray[this.dropTargetArray.length] = dropTarget;
    	this.dropTargetPositions[this.dropTargetPositions.length] = 
    		ExtrasDragSource.getElementPosition(dropTarget);
    }
}
