package io.github.klahap.pgen.dsl

import com.squareup.kotlinpoet.*


@JvmInline
value class PackageName(val name: String) {
    override fun toString(): String = name
    operator fun plus(subPackage: String) = PackageName("$name.$subPackage")
}

fun fileSpec(
    packageName: PackageName,
    name: String,
    block: FileSpec.Builder.() -> Unit,
) =
    FileSpec.builder(packageName = packageName.name, fileName = name).apply(block).build()

fun buildObject(
    name: String,
    block: TypeSpec.Builder.() -> Unit,
) = TypeSpec.objectBuilder(name).apply(block).build()

fun buildEnum(
    name: String,
    block: TypeSpec.Builder.() -> Unit,
) = TypeSpec.enumBuilder(name).apply(block).build()

fun TypeSpec.Builder.addProperty(name: String, type: TypeName, block: PropertySpec.Builder.() -> Unit) =
    addProperty(PropertySpec.builder(name = name, type = type).apply(block).build())

fun TypeSpec.Builder.addEnumConstant(
    name: String,
    block: TypeSpec.Builder.() -> Unit,
) = addEnumConstant(name, TypeSpec.anonymousClassBuilder().apply(block).build())

fun TypeSpec.Builder.primaryConstructor(
    block: FunSpec.Builder.() -> Unit,
) = primaryConstructor(FunSpec.constructorBuilder().apply(block).build())

fun TypeSpec.Builder.addCompanionObject(
    block: TypeSpec.Builder.() -> Unit,
) = addType(TypeSpec.companionObjectBuilder().apply(block).build())

fun TypeSpec.Builder.addFunction(
    name: String,
    block: FunSpec.Builder.() -> Unit,
) = addFunction(FunSpec.builder(name).apply(block).build())

fun TypeSpec.Builder.addInitializerBlock(block: CodeBlock.Builder.() -> Unit) =
    addInitializerBlock(CodeBlock.builder().apply(block).build())
