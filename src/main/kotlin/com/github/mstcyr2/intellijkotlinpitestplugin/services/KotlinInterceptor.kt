package com.github.mstcyr2.intellijkotlinpitestplugin.services

import com.intellij.openapi.components.Service
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodInsnNode
import org.pitest.bytecode.analysis.ClassTree
import org.pitest.bytecode.analysis.InstructionMatchers.*
import org.pitest.bytecode.analysis.MethodMatchers
import org.pitest.bytecode.analysis.MethodTree
import org.pitest.classinfo.ClassName
import org.pitest.mutationtest.build.InterceptorType
import org.pitest.mutationtest.build.MutationInterceptor
import org.pitest.mutationtest.engine.Mutater
import org.pitest.mutationtest.engine.MutationDetails
import org.pitest.sequence.*
import java.util.function.Predicate
import java.util.regex.Pattern


@Service(Service.Level.PROJECT)
class KotlinInterceptor : MutationInterceptor {
    private var currentClass: ClassTree? = null
    private var isKotlinClass = false

    private val DEBUG = false
    private val MUTATED_INSTRUCTION: Slot<AbstractInsnNode> = Slot.create(AbstractInsnNode::class.java)
    private val FOUND: Slot<Boolean> = Slot.create(Boolean::class.java)

    val KOTLIN_JUNK: SequenceMatcher<AbstractInsnNode> = QueryStart
            .match<AbstractInsnNode>(Match.never())
            .zeroOrMore(QueryStart.match(anyInstruction()))
            .or(destructuringCall())
            .or(nullCast())
            .or(safeNullCallOrElvis())
            .or(safeCast<SequenceQuery<AbstractInsnNode>>())
            .then(containMutation(FOUND))
            .zeroOrMore(QueryStart.match(anyInstruction()))
            .compile(QueryParams.params(AbstractInsnNode::class.java)
                    .withIgnores(notAnInstruction())
                    .withDebug(DEBUG)
            )

    private fun destructuringCall(): SequenceQuery<AbstractInsnNode?>? {
        return QueryStart
                .any(AbstractInsnNode::class.java)
                .then(aComponentNCall()!!.and(mutationPoint()))
    }

    private fun <T> safeCast(): SequenceQuery<AbstractInsnNode>? {
        val nullJump: Slot<LabelNode> = Slot.create(LabelNode::class.java)
        return QueryStart
                .any(AbstractInsnNode::class.java)
                .then(opCode(Opcodes.INSTANCEOF).and(mutationPoint()))
                .then(opCode(Opcodes.IFNE).and(jumpsTo(nullJump.write()).and(mutationPoint())))
                .then(opCode(Opcodes.POP))
                .then(opCode(Opcodes.ACONST_NULL))
                .then(labelNode(nullJump.read()))
    }

    private fun nullCast(): SequenceQuery<AbstractInsnNode>? {
        return QueryStart
                .any(AbstractInsnNode::class.java)
                .then(opCode(Opcodes.IFNONNULL).and(mutationPoint()))
                .then(methodCallTo(ClassName.fromString("kotlin/jvm/internal/Intrinsics"), "throwNpe").and(mutationPoint()))
    }

    private fun safeNullCallOrElvis(): SequenceQuery<AbstractInsnNode?>? {
        val nullJump: Slot<LabelNode> = Slot.create(LabelNode::class.java)
        return QueryStart
                .any(AbstractInsnNode::class.java)
                .then(opCode(Opcodes.IFNULL).and(jumpsTo(nullJump.write())).and(mutationPoint()))
                .oneOrMore(QueryStart.match(anyInstruction()))
                .then(opCode(Opcodes.GOTO))
                .then(labelNode(nullJump.read()))
                .then(opCode(Opcodes.POP))
                .then(aConstant()!!.and(mutationPoint()))
    }

    private fun aConstant(): Match<AbstractInsnNode?>? {
        return opCode(Opcodes.ACONST_NULL).or(anIntegerConstant().or(opCode(Opcodes.SIPUSH)).or(opCode(Opcodes.LDC)))
    }

    private fun aComponentNCall(): Match<AbstractInsnNode?>? {
        val componentPattern: Pattern = Pattern.compile("component\\d")
        return object : Match<AbstractInsnNode?> {
            override fun test(c: Context?, t: AbstractInsnNode?): Result<*>? {
                if (t is MethodInsnNode) {
                    val call: MethodInsnNode = t as MethodInsnNode
                    return Result.result(isDestructuringCall(call) && takesNoArgs(call), c)
                }
                return Result.result(false, c)
            }

            private fun isDestructuringCall(call: MethodInsnNode): Boolean {
                return takesNoArgs(call) && isComponentNCall(call)
            }

            private fun isComponentNCall(call: MethodInsnNode): Boolean {
                return componentPattern.matcher(call.name).matches()
            }

            private fun takesNoArgs(call: MethodInsnNode): Boolean {
                return call.desc.startsWith("()")
            }
        }
    }

    override fun type(): InterceptorType {
        return InterceptorType.FILTER
    }

    override fun begin(clazz: ClassTree?) {
        currentClass = clazz
        isKotlinClass = clazz!!.annotations().stream()
                .filter { annotationNode -> annotationNode.desc.equals("Lkotlin/Metadata;") }
                .findFirst()
                .isPresent
    }

    override fun intercept(mutations: MutableCollection<MutationDetails>?, m: Mutater?): MutableCollection<MutationDetails> {
        TODO("Not yet implemented")
    }

    override fun end() {
        currentClass = null;
    }

    private fun isKotlinJunkMutation(currentClass: ClassTree): Predicate<MutationDetails> {
        return Predicate<MutationDetails> { a ->
            val instruction: Int = a.getInstructionIndex()
            val method: MethodTree = currentClass.methods().stream()
                    .filter(MethodMatchers.forLocation(a.getId().getLocation()))
                    .findFirst()
                    .get()
            val mutatedInstruction: AbstractInsnNode = method.instruction(instruction)
            val context: Context = Context.start(DEBUG)
            context.store(MUTATED_INSTRUCTION.write(), mutatedInstruction)
            KOTLIN_JUNK.matches(method.instructions(), context)
        }
    }

    private fun mutationPoint(): Match<AbstractInsnNode?>? {
        return recordTarget(MUTATED_INSTRUCTION.read(), FOUND.write())
    }

    private fun containMutation(found: Slot<Boolean>): Match<AbstractInsnNode?>? {
        return Match { context, node -> Result.result(context.retrieve(found.read()).isPresent(), context) }
    }
}
