package com.github.mstcyr2.intellijkotlinpitestplugin.services

import com.github.mstcyr2.intellijkotlinpitestplugin.MyBundle
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import org.objectweb.asm.tree.AbstractInsnNode
import org.pitest.bytecode.analysis.ClassTree
import org.pitest.mutationtest.build.InterceptorType
import org.pitest.mutationtest.build.MutationInterceptor
import org.pitest.mutationtest.engine.Mutater
import org.pitest.mutationtest.engine.MutationDetails
import org.pitest.sequence.*


@Service(Service.Level.PROJECT)
class KotlinInterceptor(project: Project) : MutationInterceptor {
    private val currentClass: ClassTree? = null
    private val isKotlinClass = false

    private val DEBUG = false
    private val MUTATED_INSTRUCTION: Slot<AbstractInsnNode> = Slot.create(AbstractInsnNode::class.java)
    private val FOUND: Slot<Boolean> = Slot.create(Boolean::class.java)

//    val KOTLIN_JUNK: SequenceMatcher<AbstractInsnNode> = QueryStart
//            .match<AbstractInsnNode>(Match.never<AbstractInsnNode>())
//            .zeroOrMore(QueryStart.match<AbstractInsnNode>(anyInstruction()))
//            .or(destructuringCall())
//            .or(nullCast())
//            .or(safeNullCallOrElvis())
//            .or(safeCast<SequenceQuery<AbstractInsnNode>>())
//            .then(containMutation(FOUND))
//            .zeroOrMore(QueryStart.match<AbstractInsnNode>(anyInstruction()))
//            .compile(QueryParams.params<AbstractInsnNode>(AbstractInsnNode::class.java)
//                    .withIgnores(notAnInstruction())
//                    .withDebug(DEBUG)
//            )

    init {
        thisLogger().info(MyBundle.message("projectService", project.name))
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    fun getRandomNumber() = (1..100).random()
    override fun type(): InterceptorType {
        TODO("Not yet implemented")
    }

    override fun begin(clazz: ClassTree?) {
        TODO("Not yet implemented")
    }

    override fun intercept(mutations: MutableCollection<MutationDetails>?, m: Mutater?): MutableCollection<MutationDetails> {
        TODO("Not yet implemented")
    }

    override fun end() {
        TODO("Not yet implemented")
    }
}
