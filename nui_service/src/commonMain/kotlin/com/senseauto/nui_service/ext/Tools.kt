package com.senseauto.nui_service.ext


sealed class OutputColor(val color: String){

    val red = "\u001B[31m"
    val green = "\u001B[32m"
    val yellow = "\u001B[33m"
    val blue = "\u001B[34m"
    val reset = "\u001B[0m"
    data object Black: OutputColor("\\\u001B[30m")
    data object Red :OutputColor("\u001B[31m")
    data object Green: OutputColor("\u001B[32m")
    data object Yellow: OutputColor("\u001B[33m")
    data object Blue :OutputColor("\u001B[34m")
    data object Pink :OutputColor("\u001B[35m")
    data object Cyan : OutputColor("\u001B[36m")
    data object White:OutputColor("\u001B[37m")
    data object Reset:OutputColor("\u001B[0m")
}

fun outputln(message:Any?,color: OutputColor = OutputColor.Black){
    println(color.color+"$message"+OutputColor.Reset.color)
}

fun output(message:Any?,color: OutputColor = OutputColor.Black){
    print(color.color+"$message"+OutputColor.Reset.color)
}