class LibraryLoader(val compiler: LambdaCompiler) {
    fun insertValue(name: String, value: Any) {
        compiler.envTracker[0].add(name)
        Env[0][name.hashCode()] = value
    }
}