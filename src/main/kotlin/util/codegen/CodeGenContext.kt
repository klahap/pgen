package io.github.klahap.pgen.util.codegen

import io.github.klahap.pgen.dsl.PackageName

data class CodeGenContext(
    val rootPackageName: PackageName,
    val createDirectoriesForRootPackageName: Boolean,
)