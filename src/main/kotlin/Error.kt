data class Error(val line: Int, val col: Int, val message: String, val source: String) {
    fun print() {
        // red text
        print("\u001B[31m")
        println("at line $line column $col : $message")
        // reset text color
        print("\u001B[0m")
        val lines = source.split("\n")
        // print light gray text
        print("\u001B[37m")
        if(line - 2 >= 0)
            println("| ${lines[line - 2]}")
        // reset text color
        print("\u001B[0m")
        val line_to_print = lines[line - 1]
        println("| $line_to_print")
        print("\u001B[31m")
        for (i in 0 until col) {
            if (i >= line_to_print.length)
                break
            if(line_to_print[i] == '\t')
                print("\t")
            else
                print(" ")
        }
        // red text
        println("^")
        // print light gray text
        print("\u001B[37m")
        if(line + 1 < lines.size)
            println("| ${lines[line]}")
        // reset text color
        print("\u001B[0m")
    }
}