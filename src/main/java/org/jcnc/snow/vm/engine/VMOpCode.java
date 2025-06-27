package org.jcnc.snow.vm.engine;

import org.jcnc.snow.vm.commands.arithmetic.byte8.*;
import org.jcnc.snow.vm.commands.arithmetic.conversion.*;
import org.jcnc.snow.vm.commands.arithmetic.double64.*;
import org.jcnc.snow.vm.commands.arithmetic.float32.*;
import org.jcnc.snow.vm.commands.arithmetic.int32.*;
import org.jcnc.snow.vm.commands.arithmetic.long64.*;
import org.jcnc.snow.vm.commands.arithmetic.short16.*;
import org.jcnc.snow.vm.commands.bitwise.int32.IAndCommand;
import org.jcnc.snow.vm.commands.bitwise.int32.IOrCommand;
import org.jcnc.snow.vm.commands.bitwise.int32.IXorCommand;
import org.jcnc.snow.vm.commands.bitwise.long64.LAndCommand;
import org.jcnc.snow.vm.commands.bitwise.long64.LOrCommand;
import org.jcnc.snow.vm.commands.bitwise.long64.LXorCommand;
import org.jcnc.snow.vm.commands.control.all.JumpCommand;
import org.jcnc.snow.vm.commands.control.int32.*;
import org.jcnc.snow.vm.commands.function.CallCommand;
import org.jcnc.snow.vm.commands.function.RetCommand;
import org.jcnc.snow.vm.commands.memory.all.MovCommand;
import org.jcnc.snow.vm.commands.memory.byte8.BLoadCommand;
import org.jcnc.snow.vm.commands.memory.byte8.BStoreCommand;
import org.jcnc.snow.vm.commands.memory.double64.DLoadCommand;
import org.jcnc.snow.vm.commands.memory.double64.DStoreCommand;
import org.jcnc.snow.vm.commands.memory.float32.FLoadCommand;
import org.jcnc.snow.vm.commands.memory.float32.FStoreCommand;
import org.jcnc.snow.vm.commands.memory.int32.ILoadCommand;
import org.jcnc.snow.vm.commands.memory.int32.IStoreCommand;
import org.jcnc.snow.vm.commands.memory.long64.LLoadCommand;
import org.jcnc.snow.vm.commands.memory.long64.LStoreCommand;
import org.jcnc.snow.vm.commands.memory.short16.SLoadCommand;
import org.jcnc.snow.vm.commands.memory.short16.SStoreCommand;
import org.jcnc.snow.vm.commands.stack.all.DupCommand;
import org.jcnc.snow.vm.commands.stack.all.PopCommand;
import org.jcnc.snow.vm.commands.stack.all.SwapCommand;
import org.jcnc.snow.vm.commands.stack.byte8.BPushCommand;
import org.jcnc.snow.vm.commands.stack.double64.DPushCommand;
import org.jcnc.snow.vm.commands.stack.float32.FPushCommand;
import org.jcnc.snow.vm.commands.stack.int32.IPushCommand;
import org.jcnc.snow.vm.commands.stack.long64.LPushCommand;
import org.jcnc.snow.vm.commands.stack.short16.SPushCommand;
import org.jcnc.snow.vm.commands.vm.HaltCommand;
import org.jcnc.snow.vm.module.LocalVariableStore;

/**
 * VMOpCode defines the compact instruction set for the virtual machine, organized into logical categories.
 * <p>Each opcode represents a specific operation executed by the virtual machine.</p>
 */
public class VMOpCode {
    // region 1. Arithmetic Operations (1–100)
    // region 1.1 int32 (0-9)
    /**
     * I_ADD Opcode: Represents the int32 addition operation in the virtual machine.
     * <p>This opcode is implemented by the {@link IAddCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two int32 values from the operand stack (the first value popped is the second operand, and the second value popped is the first operand).</li>
     *     <li>Performs the addition operation on these two int32s.</li>
     *     <li>Pushes the result of the addition back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic int32 addition tasks.</p>
     */
    public static final int I_ADD = 0;
    /**
     * I_SUB Opcode: Represents the int32 subtraction operation in the virtual machine.
     * <p>This opcode is implemented by the {@link ISubCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two int32 values from the operand stack (the first value popped is the subtrahend, and the second value popped is the minuend).</li>
     *     <li>Performs the subtraction operation by subtracting the subtrahend from the minuend (i.e., <code>minuend - subtrahend</code>).</li>
     *     <li>Pushes the result of the subtraction back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic int32 subtraction tasks.</p>
     */
    public static final int I_SUB = 1;
    /**
     * I_MUL Opcode: Represents the int32 multiplication operation in the virtual machine.
     * <p>This opcode is implemented by the {@link IMulCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two int32 values from the operand stack (the first value popped is the second operand, and the second value popped is the first operand).</li>
     *     <li>Performs the multiplication operation on these two int32s (i.e., <code>firstOperand * secondOperand</code>).</li>
     *     <li>Pushes the result of the multiplication back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic int32 multiplication tasks.</p>
     */
    public static final int I_MUL = 2;
    /**
     * I_DIV Opcode: Represents the int32 division operation in the virtual machine.
     * <p>This opcode is implemented by the {@link IDivCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two int32 values from the operand stack (the first value popped is the divisor, and the second value popped is the dividend).</li>
     *     <li>Performs the division operation by dividing the dividend by the divisor (i.e., <code>dividend / divisor</code>).</li>
     *     <li>Checks for division by zero and throws an {@link ArithmeticException} if the divisor is zero, as this operation is undefined.</li>
     *     <li>Pushes the result of the division back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic int32 division tasks.</p>
     */
    public static final int I_DIV = 3;
    /**
     * I_MOD Opcode: Represents the int32 modulus (remainder) operation in the virtual machine.
     * <p>This opcode is implemented by the {@link IModCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two int32 values from the operand stack (the first value popped is the divisor, and the second value popped is the dividend).</li>
     *     <li>Performs the modulus operation by calculating the remainder of the division of the dividend by the divisor (i.e., <code>dividend % divisor</code>).</li>
     *     <li>Checks for division by zero and throws an {@link ArithmeticException} if the divisor is zero, as this operation is undefined.</li>
     *     <li>Pushes the result of the modulus operation back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic int32 modulus (remainder)  tasks.</p>
     */
    public static final int I_MOD = 4;
    /**
     * I_INC Opcode: Represents the int32 increment operation for a local variable in the virtual machine.
     * <p>This opcode is implemented by the {@link IIncCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Retrieves the index of the local variable and the increment value from the instruction parameters.</li>
     *     <li>Gets the current value of the local variable at the specified index from the local variable store.</li>
     *     <li>Increments the local variable's value by the specified increment (i.e.,
     *         <code>localVariables[index] += increment</code>).</li>
     *     <li>Updates the local variable store with the new incremented value.</li>
     *     <li>Returns the updated program counter (PC) value, typically incremented by 1, unless control flow changes.</li>
     * </ol>
     *
     * <p>This opcode is particularly useful for optimizing scenarios where a local variable, such as a counter or loop index, is frequently incremented.</p>
     */
    public static final int I_INC = 5;
    /**
     * I_NEG Opcode: Represents the int32 negation operation in the virtual machine.
     * <p>This opcode is implemented by the {@link INegCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top int32 value from the operand stack.</li>
     *     <li>Performs the negation of the popped value (i.e., <code>-value</code>).</li>
     *     <li>Pushes the negated result back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is typically used to negate an int32 value, making it a fundamental operation for arithmetic logic within the virtual machine.</p>
     */
    public static final int I_NEG = 6;
    // endregion

    // region 1.2 long64 (10-19)
    /**
     * L_ADD Opcode: Represents the long64 addition operation in the virtual machine.
     * <p>This opcode is implemented by the {@link LAddCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two long64 values from the operand stack (the first value popped is the second operand, and the second value popped is the first operand).</li>
     *     <li>Performs the addition operation on these two long64s.</li>
     *     <li>Pushes the result of the addition back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic long64 addition tasks.</p>
     */
    public static final int L_ADD = 10;
    /**
     * L_SUB Opcode: Represents the long64 subtraction operation in the virtual machine.
     * <p>This opcode is implemented by the {@link LSubCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two long64 values from the operand stack (the first value popped is the subtrahend, and the second value popped is the minuend).</li>
     *     <li>Performs the subtraction operation by subtracting the subtrahend from the minuend (i.e., <code>minuend - subtrahend</code>).</li>
     *     <li>Pushes the result of the subtraction back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic long64 subtraction tasks.</p>
     */
    public static final int L_SUB = 11;
    /**
     * L_MUL Opcode: Represents the long64 multiplication operation in the virtual machine.
     * <p>This opcode is implemented by the {@link LMulCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two long64 values from the operand stack (the first value popped is the second operand, and the second value popped is the first operand).</li>
     *     <li>Performs the multiplication operation on these two long64s (i.e., <code>firstOperand * secondOperand</code>).</li>
     *     <li>Pushes the result of the multiplication back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic long64 multiplication tasks.</p>
     */
    public static final int L_MUL = 12;
    /**
     * L_DIV Opcode: Represents the long64 division operation in the virtual machine.
     * <p>This opcode is implemented by the {@link LDivCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two long64 values from the operand stack (the first value popped is the divisor, and the second value popped is the dividend).</li>
     *     <li>Performs the division operation by dividing the dividend by the divisor (i.e., <code>dividend / divisor</code>).</li>
     *     <li>Checks for division by zero and throws an {@link ArithmeticException} if the divisor is zero, as this operation is undefined.</li>
     *     <li>Pushes the result of the division back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic long64 division tasks.</p>
     */
    public static final int L_DIV = 13;
    /**
     * L_MOD Opcode: Represents the long64 modulus (remainder) operation in the virtual machine.
     * <p>This opcode is implemented by the {@link LModCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two long64 values from the operand stack (the first value popped is the divisor, and the second value popped is the dividend).</li>
     *     <li>Performs the modulus operation by calculating the remainder of the division of the dividend by the divisor (i.e., <code>dividend % divisor</code>).</li>
     *     <li>Checks for division by zero and throws an {@link ArithmeticException} if the divisor is zero, as this operation is undefined.</li>
     *     <li>Pushes the result of the modulus operation back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic long64 modulus (remainder) tasks.</p>
     */
    public static final int L_MOD = 14;
    /**
     * L_INC Opcode: Represents the long64 increment operation for a local variable in the virtual machine.
     * <p>This opcode is implemented by the {@link LIncCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Retrieves the index of the local variable and the increment value from the instruction parameters.</li>
     *     <li>Gets the current value of the local variable at the specified index from the local variable store.</li>
     *     <li>Increments the local variable's value by the specified increment (i.e.,
     *         <code>localVariables[index] += increment</code>).</li>
     *     <li>Updates the local variable store with the new incremented value.</li>
     *     <li>Returns the updated program counter (PC) value, typically incremented by 1, unless control flow changes.</li>
     * </ol>
     *
     * <p>This opcode is particularly useful for optimizing scenarios where a local `long64` variable, such as a counter or loop index, is frequently incremented.</p>
     */
    public static final int L_INC = 15;
    /**
     * L_NEG Opcode: Represents the long64 negation operation in the virtual machine.
     * <p>This opcode is implemented by the {@link LNegCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top long64 value from the operand stack.</li>
     *     <li>Performs the negation of the popped value (i.e., <code>-value</code>).</li>
     *     <li>Pushes the negated result back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is typically used to negate a long64 value, making it a fundamental operation for arithmetic logic within the virtual machine.</p>
     */
    public static final int L_NEG = 16;
    // endregion

    // region 1.3 short16 (20-29)
    /**
     * S_ADD Opcode: Represents the short16 addition operation in the virtual machine.
     * <p>This opcode is implemented by the {@link SAddCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two short16 values from the operand stack (the first value popped is the second operand, and the second value popped is the first operand).</li>
     *     <li>Performs the addition operation on these two short16 values.</li>
     *     <li>Pushes the result of the addition back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic short16 addition tasks.</p>
     */
    public static final int S_ADD = 20;
    /**
     * S_SUB Opcode: Represents the short16 subtraction operation in the virtual machine.
     * <p>This opcode is implemented by the {@link SSubCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two short16 values from the operand stack (the first value popped is the subtrahend, and the second value popped is the minuend).</li>
     *     <li>Performs the subtraction operation by subtracting the subtrahend from the minuend (i.e., <code>minuend - subtrahend</code>).</li>
     *     <li>Pushes the result of the subtraction back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic short16 subtraction tasks.</p>
     */
    public static final int S_SUB = 21;
    /**
     * S_MUL Opcode: Represents the short16 multiplication operation in the virtual machine.
     * <p>This opcode is implemented by the {@link SMulCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two short16 values from the operand stack (the first value popped is the second operand, and the second value popped is the first operand).</li>
     *     <li>Performs the multiplication operation on these two short16 values (i.e., <code>firstOperand * secondOperand</code>).</li>
     *     <li>Pushes the result of the multiplication back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic short16 multiplication tasks.</p>
     */
    public static final int S_MUL = 22;
    /**
     * S_DIV Opcode: Represents the short16 division operation in the virtual machine.
     * <p>This opcode is implemented by the {@link SDivCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two short16 values from the operand stack (the first value popped is the divisor, and the second value popped is the dividend).</li>
     *     <li>Performs the division operation by dividing the dividend by the divisor (i.e., <code>dividend / divisor</code>).</li>
     *     <li>Checks for division by zero and throws an {@link ArithmeticException} if the divisor is zero, as this operation is undefined.</li>
     *     <li>Pushes the result of the division back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic short16 division tasks.</p>
     */
    public static final int S_DIV = 23;
    /**
     * S_MOD Opcode: Represents the short16 modulus (remainder) operation in the virtual machine.
     * <p>This opcode is implemented by the {@link SModCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two short16 values from the operand stack (the first value popped is the divisor, and the second value popped is the dividend).</li>
     *     <li>Performs the modulus operation by calculating the remainder of the division of the dividend by the divisor (i.e., <code>dividend % divisor</code>).</li>
     *     <li>Checks for division by zero and throws an {@link ArithmeticException} if the divisor is zero, as this operation is undefined.</li>
     *     <li>Pushes the result of the modulus operation back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic short16 modulus (remainder) tasks.</p>
     */
    public static final int S_MOD = 24;
    /**
     * S_INC Opcode: Represents the short16 increment operation in the virtual machine.
     * <p>This opcode is implemented by the {@link SIncCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two values from the operand stack: the first value is the index of the short16 local variable to be incremented,
     *         and the second value is the increment value to be added to the local variable.</li>
     *     <li>Increments the value of the local variable at the given index by the specified increment value (i.e.,
     *         <code>localVariables[index] += increment</code>).</li>
     *     <li>Does not affect the operand stack as the increment operation directly modifies the local variable's value.</li>
     *     <li>Executes quickly as the operation is an efficient increment of a local variable.</li>
     * </ol>
     *
     * <p>This opcode is useful for scenarios where a local variable needs to be incremented, such as counters within loops,
     * or for optimizing the modification of variables in tight loops.</p>
     */
    public static final int S_INC = 25;
    /**
     * S_NEG Opcode: Represents the short16 negation operation in the virtual machine.
     * <p>This opcode is implemented by the {@link SNegCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top short16 value from the operand stack.</li>
     *     <li>Performs the negation of the popped value (i.e., <code>-value</code>).</li>
     *     <li>Pushes the negated result back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is typically used to negate a short16 value, making it a fundamental operation for arithmetic logic within the virtual machine.</p>
     */
    public static final int S_NEG = 26;
    // endregion

    // region 1.4 byte8 (31-40)
    /**
     * B_ADD Opcode: Represents the byte8 addition operation in the virtual machine.
     * <p>This opcode is implemented by the {@link BAddCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two byte8 values from the operand stack (the first value popped is the second operand, and the second value popped is the first operand).</li>
     *     <li>Performs the addition operation on these two byte8 values.</li>
     *     <li>Pushes the result of the addition back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic byte8 addition tasks.</p>
     */
    public static final int B_ADD = 31;
    /**
     * B_SUB Opcode: Represents the byte8 subtraction operation in the virtual machine.
     * <p>This opcode is implemented by the {@link BSubCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two byte8 values from the operand stack (the first value popped is the subtrahend, and the second value popped is the minuend).</li>
     *     <li>Performs the subtraction operation by subtracting the subtrahend from the minuend (i.e., <code>minuend - subtrahend</code>).</li>
     *     <li>Pushes the result of the subtraction back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic byte8 subtraction tasks.</p>
     */
    public static final int B_SUB = 32;
    /**
     * B_MUL Opcode: Represents the byte8 multiplication operation in the virtual machine.
     * <p>This opcode is implemented by the {@link BMulCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two byte8 values from the operand stack (the first value popped is the second operand, and the second value popped is the first operand).</li>
     *     <li>Performs the multiplication operation on these two byte8 values (i.e., <code>firstOperand * secondOperand</code>).</li>
     *     <li>Pushes the result of the multiplication back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic byte8 multiplication tasks.</p>
     */
    public static final int B_MUL = 33;
    /**
     * B_DIV Opcode: Represents the byte8 division operation in the virtual machine.
     * <p>This opcode is implemented by the {@link BDivCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two byte8 values from the operand stack (the first value popped is the divisor, and the second value popped is the dividend).</li>
     *     <li>Performs the division operation by dividing the dividend by the divisor (i.e., <code>dividend / divisor</code>).</li>
     *     <li>Checks for division by zero and throws an {@link ArithmeticException} if the divisor is zero, as this operation is undefined.</li>
     *     <li>Pushes the result of the division back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic byte8 division tasks.</p>
     */
    public static final int B_DIV = 34;
    /**
     * B_MOD Opcode: Represents the byte8 modulus (remainder) operation in the virtual machine.
     * <p>This opcode is implemented by the {@link BModCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two byte8 values from the operand stack (the first value popped is the divisor, and the second value popped is the dividend).</li>
     *     <li>Performs the modulus operation by calculating the remainder of the division of the dividend by the divisor (i.e., <code>dividend % divisor</code>).</li>
     *     <li>Checks for division by zero and throws an {@link ArithmeticException} if the divisor is zero, as this operation is undefined.</li>
     *     <li>Pushes the result of the modulus operation back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic byte8 modulus (remainder) tasks.</p>
     */
    public static final int B_MOD = 35;
    /**
     * B_INC Opcode: Represents the byte8 increment operation for a local variable in the virtual machine.
     * <p>This opcode is implemented by the {@link BIncCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Retrieves the index of the local variable and the increment value from the instruction parameters.</li>
     *     <li>Gets the current value of the local variable at the specified index from the local variable store.</li>
     *     <li>Increments the local variable's value by the specified increment (i.e.,
     *         <code>localVariables[index] += increment</code>).</li>
     *     <li>Updates the local variable store with the new incremented value.</li>
     *     <li>Returns the updated program counter (PC) value, typically incremented by 1, unless control flow changes.</li>
     * </ol>
     *
     * <p>This opcode is particularly useful for optimizing scenarios where a local `byte8` variable, such as a counter or loop index, is frequently incremented.</p>
     */
    public static final int B_INC = 36;
    /**
     * B_NEG Opcode: Represents the byte8 negation operation in the virtual machine.
     * <p>This opcode is implemented by the {@link BNegCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top byte8 value from the operand stack.</li>
     *     <li>Performs the negation of the popped value (i.e., <code>-value</code>).</li>
     *     <li>Pushes the negated result back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is typically used to negate a byte8 value, making it a fundamental operation for arithmetic logic within the virtual machine.</p>
     */
    public static final int B_NEG = 37;
    // endregion

    // region 1.5 double64 (41-50)
    /**
     * D_ADD Opcode: Represents the double64 precision floating-point addition operation in the virtual machine.
     * <p>This opcode is implemented by the {@link DAddCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two double64 values from the operand stack (the first value popped is the second operand, and the second value popped is the first operand).</li>
     *     <li>Performs the double64 precision floating-point addition operation on these two operands.</li>
     *     <li>Pushes the result of the addition back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic double64 precision floating-point addition tasks.</p>
     */
    public static final int D_ADD = 41;
    /**
     * D_SUB Opcode: Represents the double64 precision floating-point subtraction operation in the virtual machine.
     * <p>This opcode is implemented by the {@link DSubCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two double64 values from the operand stack (the first value popped is the subtrahend, and the second value popped is the minuend).</li>
     *     <li>Performs the double64 precision floating-point subtraction operation by subtracting the subtrahend from the minuend (i.e., <code>minuend - subtrahend</code>).</li>
     *     <li>Pushes the result of the subtraction back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic double64 precision floating-point subtraction tasks.</p>
     */
    public static final int D_SUB = 42;
    /**
     * D_MUL Opcode: Represents the double64 precision floating-point multiplication operation in the virtual machine.
     * <p>This opcode is implemented by the {@link DMulCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two double64 values from the operand stack (the first value popped is the second operand, and the second value popped is the first operand).</li>
     *     <li>Performs the double64 precision floating-point multiplication operation on these two operands (i.e., <code>firstOperand * secondOperand</code>).</li>
     *     <li>Pushes the result of the multiplication back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic double64 precision floating-point multiplication tasks.</p>
     */
    public static final int D_MUL = 43;
    /**
     * D_DIV Opcode: Represents the double64 precision floating-point division operation in the virtual machine.
     * <p>This opcode is implemented by the {@link DDivCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two double64 values from the operand stack (the first value popped is the divisor, and the second value popped is the dividend).</li>
     *     <li>Performs the double64 precision floating-point division operation by dividing the dividend by the divisor (i.e., <code>dividend / divisor</code>).</li>
     *     <li>Checks for division by zero and throws an {@link ArithmeticException} if the divisor is zero, as this operation is undefined.</li>
     *     <li>Pushes the result of the division back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic double64 precision floating-point division tasks.</p>
     */
    public static final int D_DIV = 44;
    /**
     * D_MOD Opcode: Represents the double64 precision floating-point modulus (remainder) operation in the virtual machine.
     * <p>This opcode is implemented by the {@link DModCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two double64 values from the operand stack (the first value popped is the divisor, and the second value popped is the dividend).</li>
     *     <li>Performs the modulus operation by calculating the remainder of the division of the dividend by the divisor (i.e., <code>dividend % divisor</code>).</li>
     *     <li>Checks for division by zero and throws an {@link ArithmeticException} if the divisor is zero, as this operation is undefined.</li>
     *     <li>Pushes the result of the modulus operation back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic double64 precision floating-point modulus (remainder) tasks.</p>
     */
    public static final int D_MOD = 45;
    /**
     * D_INC Opcode: Represents the double64 increment operation for a local variable in the virtual machine.
     * <p>This opcode is implemented by the {@link DIncCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Retrieves the index of the local variable and the increment value from the instruction parameters.</li>
     *     <li>Gets the current value of the local variable at the specified index from the local variable store.</li>
     *     <li>Increments the local variable's value by the specified increment (i.e.,
     *         <code>localVariables[index] += increment</code>).</li>
     *     <li>Updates the local variable store with the new incremented value.</li>
     *     <li>Returns the updated program counter (PC) value, typically incremented by 1, unless control flow changes.</li>
     * </ol>
     *
     * <p>This opcode is particularly useful for optimizing scenarios where a local `double64` variable, such as a counter or loop index, is frequently incremented.</p>
     */
    public static final int D_INC = 46;
    /**
     * D_NEG Opcode: Represents the double64 precision floating-point negation operation in the virtual machine.
     * <p>This opcode is implemented by the {@link DNegCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top double64 value from the operand stack.</li>
     *     <li>Performs the negation of the popped value (i.e., <code>-value</code>).</li>
     *     <li>Pushes the negated result back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is typically used to negate a double64 precision floating-point value, making it a fundamental operation for double64 precision arithmetic logic within the virtual machine.</p>
     */
    public static final int D_NEG = 47;
    // endregion

    // region 1.6 float32 (51-60)
    /**
     * F_ADD Opcode: Represents the float32 addition operation in the virtual machine.
     * <p>This opcode is implemented by the {@link FAddCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two float32 values from the operand stack (the first value popped is the second operand, and the second value popped is the first operand).</li>
     *     <li>Performs the float32 addition operation on these two operands.</li>
     *     <li>Pushes the result of the addition back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic float32 addition tasks.</p>
     */
    public static final int F_ADD = 51;
    /**
     * F_SUB Opcode: Represents the float32 subtraction operation in the virtual machine.
     * <p>This opcode is implemented by the {@link FSubCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two float32 values from the operand stack (the first value popped is the subtrahend, and the second value popped is the minuend).</li>
     *     <li>Performs the float32 subtraction operation by subtracting the subtrahend from the minuend (i.e., <code>minuend - subtrahend</code>).</li>
     *     <li>Pushes the result of the subtraction back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic float32 subtraction tasks.</p>
     */
    public static final int F_SUB = 52;
    /**
     * F_MUL Opcode: Represents the float32 multiplication operation in the virtual machine.
     * <p>This opcode is implemented by the {@link FMulCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two float32 values from the operand stack (the first value popped is the second operand, and the second value popped is the first operand).</li>
     *     <li>Performs the float32 multiplication operation on these two operands (i.e., <code>firstOperand * secondOperand</code>).</li>
     *     <li>Pushes the result of the multiplication back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic float32 multiplication tasks.</p>
     */
    public static final int F_MUL = 53;
    /**
     * F_DIV Opcode: Represents the float32 division operation in the virtual machine.
     * <p>This opcode is implemented by the {@link FDivCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two float32 values from the operand stack (the first value popped is the divisor, and the second value popped is the dividend).</li>
     *     <li>Performs the float32 division operation by dividing the dividend by the divisor (i.e., <code>dividend / divisor</code>).</li>
     *     <li>Checks for division by zero and throws an {@link ArithmeticException} if the divisor is zero, as this operation is undefined.</li>
     *     <li>Pushes the result of the division back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic float32 division tasks.</p>
     */
    public static final int F_DIV = 54;
    /**
     * F_MOD Opcode: Represents the float32 modulus (remainder) operation in the virtual machine.
     * <p>This opcode is implemented by the {@link FModCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two float32 values from the operand stack (the first value popped is the divisor, and the second value popped is the dividend).</li>
     *     <li>Performs the modulus operation by calculating the remainder of the division of the dividend by the divisor (i.e., <code>dividend % divisor</code>).</li>
     *     <li>Checks for division by zero and throws an {@link ArithmeticException} if the divisor is zero, as this operation is undefined.</li>
     *     <li>Pushes the result of the modulus operation back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic float32 modulus (remainder) tasks.</p>
     */
    public static final int F_MOD = 55;
    /**
     * F_INC Opcode: Represents the float32 increment operation for a local variable in the virtual machine.
     * <p>This opcode is implemented by the {@link FIncCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Retrieves the index of the local variable and the increment value from the instruction parameters.</li>
     *     <li>Gets the current value of the local variable at the specified index from the local variable store.</li>
     *     <li>Increments the local variable's value by the specified increment (i.e.,
     *         <code>localVariables[index] += increment</code>).</li>
     *     <li>Updates the local variable store with the new incremented value.</li>
     *     <li>Returns the updated program counter (PC) value, typically incremented by 1, unless control flow changes.</li>
     * </ol>
     *
     * <p>This opcode is particularly useful for optimizing scenarios where a local `float32` variable, such as a counter or loop index, is frequently incremented.</p>
     */
    public static final int F_INC = 56;
    /**
     * F_NEG Opcode: Represents the float32 negation operation in the virtual machine.
     * <p>This opcode is implemented by the {@link FNegCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top float32 value from the operand stack.</li>
     *     <li>Performs the negation of the popped value (i.e., <code>-value</code>).</li>
     *     <li>Pushes the negated result back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is typically used to negate a float32 value, making it a fundamental operation for float32 arithmetic logic within the virtual machine.</p>
     */
    public static final int F_NEG = 57;
    // endregion
    // endregion

    // region 2. Type Conversion Operation
    /**
     * I2L Opcode: Represents the type conversion operation from int32 to long64 in the virtual machine.
     * <p>This opcode is implemented by the {@link I2LCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pop the top int32 value from the operand stack.</li>
     *     <li>Convert the int32 value to a long64 value.</li>
     *     <li>Push the converted long64 value back onto the operand stack for subsequent operations.</li>
     * </ol>
     *
     * <p>This opcode is commonly used to widen an int32 value to a long64 type to accommodate larger numeric ranges.</p>
     */
    public static final int I2L = 61;
    /**
     * I2S Opcode: Represents the type conversion operation from int32 to short16 in the virtual machine.
     * <p>This opcode is implemented by the {@link I2SCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pop the top int32 value from the operand stack.</li>
     *     <li>Convert the int32 value to a short16 value (this may involve truncation).</li>
     *     <li>Push the converted short16 value back onto the operand stack for subsequent operations.</li>
     * </ol>
     *
     * <p>This opcode is typically used to narrow an int32 value to a short16 type when a smaller data representation is needed.</p>
     */
    public static final int I2S = 62;
    /**
     * I2B Opcode: Represents the type conversion operation from int32 to byte8 in the virtual machine.
     * <p>This opcode is implemented by the {@link I2BCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pop the top int32 value from the operand stack.</li>
     *     <li>Convert the int32 value to a byte8 value (this may involve truncation).</li>
     *     <li>Push the converted byte8 value back onto the operand stack for subsequent operations.</li>
     * </ol>
     *
     * <p>This opcode is used to narrow an int32 value to a byte8 type, suitable when a smaller numeric type is required.</p>
     */
    public static final int I2B = 63;
    /**
     * I2D Opcode: Represents the type conversion operation from int32 to double64 in the virtual machine.
     * <p>This opcode is implemented by the {@link I2DCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pop the top int32 value from the operand stack.</li>
     *     <li>Convert the int32 value to a double64 value.</li>
     *     <li>Push the converted double64 value back onto the operand stack for subsequent operations.</li>
     * </ol>
     *
     * <p>This opcode is used to widen an int32 value to a double64 type, providing high-precision floating-point calculations.</p>
     */
    public static final int I2D = 64;
    /**
     * I2F Opcode: Represents the type conversion operation from int32 to float32 in the virtual machine.
     * <p>This opcode is implemented by the {@link I2FCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pop the top int32 value from the operand stack.</li>
     *     <li>Convert the int32 value to a float32 value.</li>
     *     <li>Push the converted float32 value back onto the operand stack for subsequent operations.</li>
     * </ol>
     *
     * <p>This opcode is used to convert an int32 value to a float32 type when floating-point arithmetic is required.</p>
     */
    public static final int I2F = 65;
    /**
     * L2I Opcode: Represents the type conversion operation from long64 to int32 in the virtual machine.
     * <p>This opcode is implemented by the {@link L2ICommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pop the top long64 value from the operand stack.</li>
     *     <li>Convert the long64 value to an int32 value (this may involve truncation).</li>
     *     <li>Push the converted int32 value back onto the operand stack for subsequent operations.</li>
     * </ol>
     *
     * <p>This opcode is typically used to narrow a long64 value to an int32 type for further integer operations.</p>
     */
    public static final int L2I = 66;
    /**
     * L2D Opcode: Represents the type conversion operation from long64 to double64 in the virtual machine.
     * <p>This opcode is implemented by the {@link L2DCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pop the top long64 value from the operand stack.</li>
     *     <li>Convert the long64 value to a double64 value.</li>
     *     <li>Push the converted double64 value back onto the operand stack for subsequent operations.</li>
     * </ol>
     *
     * <p>This opcode is used to widen a long64 value to a double64 type for high-precision floating-point computations.</p>
     */
    public static final int L2D = 67;

    /**
     * L2F Opcode: Represents the type conversion operation from long64 to float32 in the virtual machine.
     * <p>This opcode is implemented by the {@link L2FCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pop the top long64 value from the operand stack.</li>
     *     <li>Convert the long64 value to a float32 value.</li>
     *     <li>Push the converted float32 value back onto the operand stack for subsequent operations.</li>
     * </ol>
     *
     * <p>This opcode is used to convert a long64 value to a float32 type, typically for floating-point arithmetic involving long values.</p>
     */
    public static final int L2F = 68;

    /**
     * F2I Opcode: Represents the type conversion operation from float32 to int32 in the virtual machine.
     * <p>This opcode is implemented by the {@link F2ICommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pop the top float32 value from the operand stack.</li>
     *     <li>Convert the float32 value to an int32 value (this may involve truncation).</li>
     *     <li>Push the converted int32 value back onto the operand stack for subsequent operations.</li>
     * </ol>
     *
     * <p>This opcode is used to convert a float32 value to an int32 type for further integer-based operations or comparisons.</p>
     */
    public static final int F2I = 69;

    /**
     * F2L Opcode: Represents the type conversion operation from float32 to long64 in the virtual machine.
     * <p>This opcode is implemented by the {@link F2LCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pop the top float32 value from the operand stack.</li>
     *     <li>Convert the float32 value to a long64 value.</li>
     *     <li>Push the converted long64 value back onto the operand stack for subsequent operations.</li>
     * </ol>
     *
     * <p>This opcode is used to widen a float32 value to a long64 type, which is useful when operations require a larger numeric range.</p>
     */
    public static final int F2L = 70;

    /**
     * F2D Opcode: Represents the type conversion operation from float32 to double64 in the virtual machine.
     * <p>This opcode is implemented by the {@link F2DCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pop the top float32 value from the operand stack.</li>
     *     <li>Convert the float32 value to a double64 value.</li>
     *     <li>Push the converted double64 value back onto the operand stack for subsequent operations.</li>
     * </ol>
     *
     * <p>This opcode is used to promote a float32 value to a double64 type, thereby increasing precision for floating-point computations.</p>
     */
    public static final int F2D = 71;

    /**
     * D2I Opcode: Represents the type conversion operation from double64 to int32 in the virtual machine.
     * <p>This opcode is implemented by the {@link D2ICommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pop the top double64 value from the operand stack.</li>
     *     <li>Convert the double64 value to an int32 value (this may involve truncation).</li>
     *     <li>Push the converted int32 value back onto the operand stack for subsequent operations.</li>
     * </ol>
     *
     * <p>This opcode is used to narrow a double64 value to an int32 type for further integer-based processing.</p>
     */
    public static final int D2I = 72;

    /**
     * D2L Opcode: Represents the type conversion operation from double64 to long64 in the virtual machine.
     * <p>This opcode is implemented by the {@link D2LCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pop the top double64 value from the operand stack.</li>
     *     <li>Convert the double64 value to a long64 value (this may involve truncation).</li>
     *     <li>Push the converted long64 value back onto the operand stack for subsequent operations.</li>
     * </ol>
     *
     * <p>This opcode is used to narrow a double64 value to a long64 type, which can then be used for integer operations.</p>
     */
    public static final int D2L = 73;

    /**
     * D2F Opcode: Represents the type conversion operation from double64 to float32 in the virtual machine.
     * <p>This opcode is implemented by the {@link D2FCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pop the top double64 value from the operand stack.</li>
     *     <li>Convert the double64 value to a float32 value.</li>
     *     <li>Push the converted float32 value back onto the operand stack for subsequent operations.</li>
     * </ol>
     *
     * <p>This opcode is used to narrow a double64 value to a float32 type when lower precision floating-point arithmetic is acceptable.</p>
     */
    public static final int D2F = 74;

    /**
     * S2I Opcode: Represents the type conversion operation from short16 to int32 in the virtual machine.
     * <p>This opcode is implemented by the {@link S2ICommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pop the top short16 value from the operand stack.</li>
     *     <li>Convert the short16 value to an int32 value.</li>
     *     <li>Push the converted int32 value back onto the operand stack for subsequent operations.</li>
     * </ol>
     *
     * <p>This opcode is used to widen a short16 value to an int32 type, facilitating subsequent integer arithmetic or comparison operations.</p>
     */
    public static final int S2I = 75;

    /**
     * B2I Opcode: Represents the type conversion operation from byte8 to int32 in the virtual machine.
     * <p>This opcode is implemented by the {@link B2ICommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pop the top byte8 value from the operand stack.</li>
     *     <li>Convert the byte8 value to an int32 value.</li>
     *     <li>Push the converted int32 value back onto the operand stack for subsequent operations.</li>
     * </ol>
     *
     * <p>This opcode is used to widen a byte8 value to an int32 type to ensure compatibility with integer-based operations.</p>
     */
    public static final int B2I = 76;
    // endregion

    // region 2. Bitwise Operations (81–90)
    // region 2.1 int32 (81-85)
    /**
     * I_AND Opcode: Represents the int32 bitwise AND operation in the virtual machine.
     * <p>This opcode is implemented by the {@link IAndCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two int32 values from the operand stack. These values are treated as 32-bit binary representations.</li>
     *     <li>Performs the int32 bitwise AND operation on the two popped operands. The operation compares corresponding bits of both operands:
     *         <ul>
     *             <li>Each bit in the result is set to <code>1</code> only if the corresponding bits in both operands are also <code>1</code>.</li>
     *             <li>If either of the corresponding bits is <code>0</code>, the resulting bit is <code>0</code>.</li>
     *         </ul>
     *     </li>
     *     <li>Pushes the result of the int32 bitwise AND operation back onto the operand stack for further processing.</li>
     * </ol>
     *
     * <p>This opcode is essential for low-level bit manipulation tasks.</p>
     */
    public static final int I_AND = 81;
    /**
     * I_OR Opcode: Represents the int32 bitwise OR operation in the virtual machine.
     * <p>This opcode is implemented by the {@link IOrCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two int32 values from the operand stack. These values are treated as 32-bit binary representations.</li>
     *     <li>Performs the int32 bitwise OR operation on the two popped operands. The operation compares corresponding bits of both operands:
     *         <ul>
     *             <li>Each bit in the result is set to <code>1</code> if at least one of the corresponding bits in either operand is <code>1</code>.</li>
     *             <li>If both corresponding bits are <code>0</code>, the resulting bit is <code>0</code>.</li>
     *         </ul>
     *     </li>
     *     <li>Pushes the result of the int32 bitwise OR operation back onto the operand stack for further processing.</li>
     * </ol>
     *
     * <p>This opcode is essential for low-level bit manipulation tasks.</p>
     */
    public static final int I_OR = 82;

    /**
     * I_XOR Opcode: Represents the int32 bitwise XOR operation in the virtual machine.
     * <p>This opcode is implemented by the {@link IXorCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two int32 values from the operand stack. These values are treated as 32-bit binary representations.</li>
     *     <li>Performs the int32 bitwise XOR (exclusive OR) operation on the two operands:
     *         <ul>
     *             <li>If the corresponding bits are different, the result is <code>1</code>.</li>
     *             <li>If the corresponding bits are the same, the result is <code>0</code>.</li>
     *         </ul>
     *     </li>
     *     <li>Pushes the result of the int32 bitwise XOR operation back onto the operand stack for further processing.</li>
     * </ol>
     *
     * <p>This opcode is essential for low-level bit manipulation tasks.</p>
     */
    public static final int I_XOR = 83;

    // endregion
    // region 2.2 Long64 (86-90)
    /**
     * L_AND Opcode: Represents the long64 bitwise AND operation in the virtual machine.
     * <p>This opcode is implemented by the {@link LAndCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two long64 values from the operand stack. These values are treated as 32-bit binary representations.</li>
     *     <li>Performs the long64 bitwise AND operation on the two popped operands. The operation compares corresponding bits of both operands:
     *         <ul>
     *             <li>Each bit in the result is set to <code>1</code> only if the corresponding bits in both operands are also <code>1</code>.</li>
     *             <li>If either of the corresponding bits is <code>0</code>, the resulting bit is <code>0</code>.</li>
     *         </ul>
     *     </li>
     *     <li>Pushes the result of the long64 bitwise AND operation back onto the operand stack for further processing.</li>
     * </ol>
     *
     * <p>This opcode is essential for low-level bit manipulation tasks.</p>
     */
    public static final int L_AND = 86;
    /**
     * L_OR Opcode: Represents the long64 bitwise OR operation in the virtual machine.
     * <p>This opcode is implemented by the {@link LOrCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two long64 values from the operand stack. These values are treated as 32-bit binary representations.</li>
     *     <li>Performs the long64 bitwise OR operation on the two popped operands. The operation compares corresponding bits of both operands:
     *         <ul>
     *             <li>Each bit in the result is set to <code>1</code> if at least one of the corresponding bits in either operand is <code>1</code>.</li>
     *             <li>If both corresponding bits are <code>0</code>, the resulting bit is <code>0</code>.</li>
     *         </ul>
     *     </li>
     *     <li>Pushes the result of the long64 bitwise OR operation back onto the operand stack for further processing.</li>
     * </ol>
     *
     * <p>This opcode is essential for low-level bit manipulation tasks.</p>
     */
    public static final int L_OR = 87;

    /**
     * L_XOR Opcode: Represents the long64 bitwise XOR operation in the virtual machine.
     * <p>This opcode is implemented by the {@link LXorCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two long64 values from the operand stack. These values are treated as 32-bit binary representations.</li>
     *     <li>Performs the long64 bitwise XOR (exclusive OR) operation on the two operands:
     *         <ul>
     *             <li>If the corresponding bits are different, the result is <code>1</code>.</li>
     *             <li>If the corresponding bits are the same, the result is <code>0</code>.</li>
     *         </ul>
     *     </li>
     *     <li>Pushes the result of the long64 bitwise XOR operation back onto the operand stack for further processing.</li>
     * </ol>
     *
     * <p>This opcode is essential for low-level bit manipulation tasks.</p>
     */
    public static final int L_XOR = 88;


    // endregion
    // endregion
    // region 3. Control Flow Operations (91–110)
    // region 3.1 JUMP (91-91)
    /**
     * JUMP Opcode: Represents an unconditional jump to a target instruction address.
     * <p>This opcode is implemented by the {@link JumpCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Validates the target address to ensure it is a non-negative int32.</li>
     *     <li>If the target address is valid (greater than or equal to 0), updates the program counter (PC) to the specified target address,
     *         effectively skipping all intermediate instructions.</li>
     *     <li>If the target address is invalid (less than 0), logs an error message and halts execution by returning an invalid value (typically -1).</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Control flow management in virtual machine execution.</li>
     * </ul>
     */
    public static final int JUMP = 91;

    // endregion
    // region 3.2 int32 (92-97)
    /**
     * IC_E Opcode: Represents a conditional jump based on int32 equality.
     * <p>This opcode is implemented by the {@link ICECommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two int32 values from the operand stack.</li>
     *     <li>Compares the two int32s for equality.</li>
     *     <li>If the int32s are equal, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the int32s are not equal, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on int32 comparison.</li>
     *     <li>Implementing control flow structures such as if-statements and loops.</li>
     * </ul>
     */
    public static final int IC_E = 92;
    /**
     * IC_NE Opcode: Represents a conditional jump based on int32 inequality.
     * <p>This opcode is implemented by the {@link ICNECommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two int32 values from the operand stack.</li>
     *     <li>Compares the two int32s for inequality.</li>
     *     <li>If the int32s are not equal, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the int32s are equal, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on int32 comparison.</li>
     *     <li>Implementing control flow structures such as conditional loops and if-else statements.</li>
     * </ul>
     */
    public static final int IC_NE = 93;
    /**
     * IC_G Opcode: Represents a conditional jump based on int32 comparison (greater than).
     * <p>This opcode is implemented by the {@link ICGCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two int32 values from the operand stack.</li>
     *     <li>Compares the first int32 with the second to determine if it is greater.</li>
     *     <li>If the first int32 is greater than the second, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the first int32 is not greater than the second, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on int32 comparison.</li>
     *     <li>Implementing control flow structures such as greater-than conditions in loops and conditional statements.</li>
     * </ul>
     */
    public static final int IC_G = 94;
    /**
     * IC_GE Opcode: Represents a conditional jump based on int32 comparison (greater than or equal to).
     * <p>This opcode is implemented by the {@link ICGECommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two int32 values from the operand stack.</li>
     *     <li>Compares the first int32 with the second to determine if it is greater than or equal to the second int32.</li>
     *     <li>If the first int32 is greater than or equal to the second, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the first int32 is less than the second, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on int32 comparison.</li>
     *     <li>Implementing control flow structures such as loops, conditional statements, and range checks.</li>
     * </ul>
     */
    public static final int IC_GE = 95;
    /**
     * IC_L Opcode: Represents a conditional jump based on int32 comparison (less than).
     * <p>This opcode is implemented by the {@link ICLCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two int32 values from the operand stack.</li>
     *     <li>Compares the first int32 with the second to determine if it is less than the second int32.</li>
     *     <li>If the first int32 is less than the second, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the first int32 is greater than or equal to the second, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on int32 comparison.</li>
     *     <li>Implementing control flow structures such as loops, conditional statements, and range validations.</li>
     * </ul>
     */
    public static final int IC_L = 96;
    /**
     * IC_LE Opcode: Represents a conditional jump based on int32 comparison (less than or equal).
     * <p>This opcode is implemented by the {@link ICLECommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two int32 values from the operand stack.</li>
     *     <li>Compares the first int32 with the second to determine if it is less than or equal to the second int32.</li>
     *     <li>If the first int32 is less than or equal to the second, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the first int32 is greater than the second, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on int32 comparison.</li>
     *     <li>Implementing control flow structures such as loops, conditional statements, and boundary checks.</li>
     * </ul>
     */
    public static final int IC_LE = 97;

    // endregion
    // region 3.3 long64 (98-103)
    /**
     * LC_E Opcode: Represents a conditional jump based on long64 equality.
     * <p>This opcode is implemented by the {@link ICECommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two long64 values from the operand stack.</li>
     *     <li>Compares the two long64s for equality.</li>
     *     <li>If the long64s are equal, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the long64s are not equal, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on long64 comparison.</li>
     *     <li>Implementing control flow structures such as if-statements and loops.</li>
     * </ul>
     */
    public static final int LC_E = 98;
    /**
     * LC_NE Opcode: Represents a conditional jump based on long64 inequality.
     * <p>This opcode is implemented by the {@link ICNECommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two long64 values from the operand stack.</li>
     *     <li>Compares the two long64s for inequality.</li>
     *     <li>If the long64s are not equal, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the long64s are equal, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on long64 comparison.</li>
     *     <li>Implementing control flow structures such as conditional loops and if-else statements.</li>
     * </ul>
     */
    public static final int LC_NE = 99;
    /**
     * LC_G Opcode: Represents a conditional jump based on long64 comparison (greater than).
     * <p>This opcode is implemented by the {@link ICGCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two long64 values from the operand stack.</li>
     *     <li>Compares the first long64 with the second to determine if it is greater.</li>
     *     <li>If the first long64 is greater than the second, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the first long64 is not greater than the second, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on long64 comparison.</li>
     *     <li>Implementing control flow structures such as greater-than conditions in loops and conditional statements.</li>
     * </ul>
     */
    public static final int LC_G = 100;
    /**
     * LC_GE Opcode: Represents a conditional jump based on long64 comparison (greater than or equal to).
     * <p>This opcode is implemented by the {@link ICGECommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two long64 values from the operand stack.</li>
     *     <li>Compares the first long64 with the second to determine if it is greater than or equal to the second long64.</li>
     *     <li>If the first long64 is greater than or equal to the second, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the first long64 is less than the second, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on long64 comparison.</li>
     *     <li>Implementing control flow structures such as loops, conditional statements, and range checks.</li>
     * </ul>
     */
    public static final int LC_GE = 101;
    /**
     * LC_L Opcode: Represents a conditional jump based on long64 comparison (less than).
     * <p>This opcode is implemented by the {@link ICLCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two long64 values from the operand stack.</li>
     *     <li>Compares the first long64 with the second to determine if it is less than the second long64.</li>
     *     <li>If the first long64 is less than the second, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the first long64 is greater than or equal to the second, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on long64 comparison.</li>
     *     <li>Implementing control flow structures such as loops, conditional statements, and range validations.</li>
     * </ul>
     */
    public static final int LC_L = 102;
    /**
     * LC_LE Opcode: Represents a conditional jump based on long64 comparison (less than or equal).
     * <p>This opcode is implemented by the {@link ICLECommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two long64 values from the operand stack.</li>
     *     <li>Compares the first long64 with the second to determine if it is less than or equal to the second long64.</li>
     *     <li>If the first long64 is less than or equal to the second, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the first long64 is greater than the second, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on long64 comparison.</li>
     *     <li>Implementing control flow structures such as loops, conditional statements, and boundary checks.</li>
     * </ul>
     */
    public static final int LC_LE = 103;

    // endregion
    // endregion
    // region 4. Stack Operations (111–150)
    // region 4.1 PUSH (111-120)
    /**
     * I_PUSH Opcode: Represents a stack operation that pushes an int32 value onto the operand stack.
     * <p>This opcode is implemented by the {@link IPushCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the int32 value from the instruction parameters.</li>
     *     <li>Pushes the parsed int32 value onto the operand stack.</li>
     *     <li>Increments the program counter (PC) to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Loading constant values into the operand stack for later operations.</li>
     *     <li>Preparing operands for arithmetic, logical, or comparison instructions.</li>
     *     <li>Facilitating method calls or control flow by managing stack-based data.</li>
     * </ul>
     */
    public static final int I_PUSH = 111;
    /**
     * L_PUSH Opcode: Represents a stack operation that pushes a long64 value onto the operand stack.
     * <p>This opcode is implemented by the {@link LPushCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the long64 value from the instruction parameters.</li>
     *     <li>Pushes the parsed long64 value onto the operand stack.</li>
     *     <li>Increments the program counter (PC) to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Loading constant values into the operand stack for later operations.</li>
     *     <li>Preparing operands for arithmetic, logical, or comparison instructions.</li>
     *     <li>Facilitating method calls or control flow by managing stack-based data.</li>
     * </ul>
     */
    public static final int L_PUSH = 112;
    /**
     * S_PUSH Opcode: Represents a stack operation that pushes a short16 value onto the operand stack.
     * <p>This opcode is implemented by the {@link SPushCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the short16 value from the instruction parameters.</li>
     *     <li>Pushes the parsed short16 value onto the operand stack.</li>
     *     <li>Increments the program counter (PC) to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Loading constant values into the operand stack for later operations.</li>
     *     <li>Preparing operands for arithmetic, logical, or comparison instructions.</li>
     *     <li>Facilitating method calls or control flow by managing stack-based data.</li>
     * </ul>
     */
    public static final int S_PUSH = 113;
    /**
     * I_PUSH Opcode: Represents a stack operation that pushes a byte8 value onto the operand stack.
     * <p>This opcode is implemented by the {@link BPushCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the byte8 value from the instruction parameters.</li>
     *     <li>Pushes the parsed byte8 value onto the operand stack.</li>
     *     <li>Increments the program counter (PC) to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Loading constant values into the operand stack for later operations.</li>
     *     <li>Preparing operands for arithmetic, logical, or comparison instructions.</li>
     *     <li>Facilitating method calls or control flow by managing stack-based data.</li>
     * </ul>
     */
    public static final int B_PUSH = 114;
    /**
     * I_PUSH Opcode: Represents a stack operation that pushes a double64 value onto the operand stack.
     * <p>This opcode is implemented by the {@link DPushCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the double64 value from the instruction parameters.</li>
     *     <li>Pushes the parsed double64 value onto the operand stack.</li>
     *     <li>Increments the program counter (PC) to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Loading constant values into the operand stack for later operations.</li>
     *     <li>Preparing operands for arithmetic, logical, or comparison instructions.</li>
     *     <li>Facilitating method calls or control flow by managing stack-based data.</li>
     * </ul>
     */
    public static final int D_PUSH = 115;
    /**
     * F_PUSH Opcode: Represents a stack operation that pushes a float32 value onto the operand stack.
     * <p>This opcode is implemented by the {@link FPushCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the float32 value from the instruction parameters.</li>
     *     <li>Pushes the parsed float32 value onto the operand stack.</li>
     *     <li>Increments the program counter (PC) to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Loading constant values into the operand stack for later operations.</li>
     *     <li>Preparing operands for arithmetic, logical, or comparison instructions.</li>
     *     <li>Facilitating method calls or control flow by managing stack-based data.</li>
     * </ul>
     */
    public static final int F_PUSH = 116;
    // endregion
    // region 4.2 POP (121-125)
    /**
     * I_POP Opcode: Represents a stack operation that removes the top element from the operand stack.
     * <p>This opcode is implemented by the {@link PopCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Removes (pops) the top element from the operand stack.</li>
     *     <li>Discards the popped value, as it is not stored or used further.</li>
     *     <li>Increments the program counter (PC) to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Clearing temporary or unnecessary data from the operand stack.</li>
     *     <li>Managing stack cleanup after operations that leave excess data.</li>
     *     <li>Ensuring stack balance during function calls or control flow transitions.</li>
     * </ul>
     */
    public static final int POP = 121;
    // endregion
    // region 4.3 DUP (126-130)
    /**
     * DUP Opcode: Represents a stack operation that duplicates the top element of the operand stack.
     * <p>This opcode is implemented by the {@link DupCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Retrieves the top element from the operand stack.</li>
     *     <li>Duplicates the top value and pushes it back onto the stack.</li>
     *     <li>Increments the program counter (PC) to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Preserving the top element of the operand stack for later use in later operations.</li>
     *     <li>Duplicating values that are needed multiple times in the execution flow.</li>
     *     <li>Managing stack balance when performing operations that require repeated access to the same data.</li>
     * </ul>
     */

    public static final int DUP = 126;
    // endregion

    // region 4.4 SWAP (131-135)
    /**
     * SWAP Opcode: Represents a stack operation that swaps the top two values of the operand stack.
     * <p>This opcode is implemented by the {@link SwapCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Ensures that there are at least two elements on the operand stack.</li>
     *     <li>Pops the two topmost values from the stack.</li>
     *     <li>Pushes the two values back onto the stack in reversed order.</li>
     *     <li>Increments the program counter (PC) to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Reversing the order of the top two elements on the stack, which may be required in certain algorithms or operations.</li>
     *     <li>Handling data rearrangements that require immediate swapping of operands during execution.</li>
     *     <li>Ensuring proper operand placement for later instructions that depend on the order of stack elements.</li>
     * </ul>
     */
    public static final int SWAP = 131;
    // endregion

    // endregion
    // region 5. Memory Operations (151–166)
    /**
     * I_STORE Opcode: Represents a store operation that saves an int32 value from the operand stack into the local variable store.
     *
     * <p>This opcode is implemented by the {@link IStoreCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Retrieves the index for the local variable from the instruction parameters.</li>
     *     <li>Pops the int32 value from the operand stack.</li>
     *     <li>Stores the value into the local variable store at the specified index of the current method frame.</li>
     *     <li>Increments the program counter (PC) to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Storing results of computations or intermediate values into local variables.</li>
     *     <li>Preserving state across instructions or method calls by saving values to the local frame.</li>
     *     <li>Transferring values from the operand stack to the method-local scope.</li>
     * </ul>
     */
    public static final int I_STORE = 151;
    /**
     * L_STORE Opcode: Represents a store operation that stores a long64 value from the operand stack into the local variable store.
     * <p>This opcode is implemented by the {@link LStoreCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Retrieves the index for the local variable from the instruction parameters.</li>
     *     <li>Pops a long64 value from the operand stack.</li>
     *     <li>Stores the value into the local variable store at the specified index of the current method frame.</li>
     *     <li>Increments the program counter (PC) to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Storing computed long64 values into local variables for reuse.</li>
     *     <li>Preserving long-type data across multiple instructions or calls.</li>
     *     <li>Moving data from the operand stack to a persistent method-local context.</li>
     * </ul>
     */
    public static final int L_STORE = 152;
    /**
     * S_STORE Opcode: Represents a store operation that stores a short16 value from the operand stack into the local variable store.
     * <p>This opcode is implemented by the {@link SStoreCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Retrieves the index for the local variable from the instruction parameters.</li>
     *     <li>Pops a short16 value from the operand stack.</li>
     *     <li>Stores the value into the local variable store at the specified index of the current method frame.</li>
     *     <li>Increments the program counter (PC) to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Storing short16 values resulting from calculations or conversions.</li>
     *     <li>Temporarily saving data in local variables for later instructions.</li>
     *     <li>Supporting typed local variable storage for short values.</li>
     * </ul>
     */
    public static final int S_STORE = 153;
    /**
     * B_STORE Opcode: Represents a store operation that stores a byte8 value from the operand stack into the local variable store.
     * <p>This opcode is implemented by the {@link BStoreCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Retrieves the index for the local variable from the instruction parameters.</li>
     *     <li>Pops a byte8 value from the operand stack.</li>
     *     <li>Stores the value into the local variable store at the specified index of the current method frame.</li>
     *     <li>Increments the program counter (PC) to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Saving byte8 results or constants into method-local variables.</li>
     *     <li>Enabling byte-level operations and temporary storage.</li>
     *     <li>Transferring values from the stack to local scope in compact form.</li>
     * </ul>
     */
    public static final int B_STORE = 154;
    /**
     * D_STORE Opcode: Represents a store operation that stores a double64 value from the operand stack into the local variable store.
     * <p>This opcode is implemented by the {@link DStoreCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Retrieves the index for the local variable from the instruction parameters.</li>
     *     <li>Pops a double64 value from the operand stack.</li>
     *     <li>Stores the value into the local variable store at the specified index of the current method frame.</li>
     *     <li>Increments the program counter (PC) to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Saving double64-precision results after arithmetic or conversion operations.</li>
     *     <li>Ensuring floating-point values persist in method-local storage.</li>
     *     <li>Managing precision-critical calculations across instruction sequences.</li>
     * </ul>
     */
    public static final int D_STORE = 155;
    /**
     * F_STORE Opcode: Represents a store operation that stores a float32 value from the operand stack into the local variable store.
     * <p>This opcode is implemented by the {@link FStoreCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Retrieves the index for the local variable from the instruction parameters.</li>
     *     <li>Pops a float32 value from the operand stack.</li>
     *     <li>Stores the value into the local variable store at the specified index of the current method frame.</li>
     *     <li>Increments the program counter (PC) to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Saving float32 results into method-local variables.</li>
     *     <li>Supporting floating-point local variable operations.</li>
     *     <li>Preserving floating-point values between instructions or calls.</li>
     * </ul>
     */
    public static final int F_STORE = 156;
    /**
     * I_LOAD Opcode: Represents a load operation that retrieves an int32 value from the local variable store and pushes it onto the operand stack.
     * <p>This opcode is implemented by the {@link ILoadCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Retrieves the index for the local variable from the instruction parameters.</li>
     *     <li>Retrieves the corresponding value from the local variable store of the current method frame.</li>
     *     <li>Pushes the retrieved value onto the operand stack for subsequent operations.</li>
     *     <li>Increments the program counter (PC) to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Loading local variables onto the operand stack for further operations or computations.</li>
     *     <li>Retrieving stored values that are needed for subsequent instructions, such as arithmetic or logic operations.</li>
     *     <li>Preserving the necessary method-local state by pushing relevant values from the local variable store to the operand stack.</li>
     * </ul>
     */
    public static final int I_LOAD = 161;
    /**
     * L_LOAD Opcode: Represents a load operation that retrieves a long64 value from the local variable store and pushes it onto the operand stack.
     * <p>This opcode is implemented by the {@link LLoadCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Retrieves the index for the local variable from the instruction parameters.</li>
     *     <li>Retrieves the corresponding value from the local variable store of the current method frame.</li>
     *     <li>Pushes the retrieved value onto the operand stack for subsequent operations.</li>
     *     <li>Increments the program counter (PC) to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Loading local variables onto the operand stack for further operations or computations.</li>
     *     <li>Retrieving stored values that are needed for subsequent instructions, such as arithmetic or logic operations.</li>
     *     <li>Preserving the necessary method-local state by pushing relevant values from the local variable store to the operand stack.</li>
     * </ul>
     */
    public static final int L_LOAD = 162;
    /**
     * S_LOAD Opcode: Represents a load operation that retrieves a short16 value from the local variable store and pushes it onto the operand stack.
     * <p>This opcode is implemented by the {@link SLoadCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Retrieves the index for the local variable from the instruction parameters.</li>
     *     <li>Retrieves the corresponding value from the local variable store of the current method frame.</li>
     *     <li>Pushes the retrieved value onto the operand stack for subsequent operations.</li>
     *     <li>Increments the program counter (PC) to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Loading local variables onto the operand stack for further operations or computations.</li>
     *     <li>Retrieving stored values that are needed for subsequent instructions, such as arithmetic or logic operations.</li>
     *     <li>Preserving the necessary method-local state by pushing relevant values from the local variable store to the operand stack.</li>
     * </ul>
     */
    public static final int S_LOAD = 163;
    /**
     * B_LOAD Opcode: Represents a load operation that retrieves a byte8 value from the local variable store and pushes it onto the operand stack.
     * <p>This opcode is implemented by the {@link BLoadCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Retrieves the index for the local variable from the instruction parameters.</li>
     *     <li>Retrieves the corresponding value from the local variable store of the current method frame.</li>
     *     <li>Pushes the retrieved value onto the operand stack for subsequent operations.</li>
     *     <li>Increments the program counter (PC) to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Loading local variables onto the operand stack for further operations or computations.</li>
     *     <li>Retrieving stored values that are needed for subsequent instructions, such as arithmetic or logic operations.</li>
     *     <li>Preserving the necessary method-local state by pushing relevant values from the local variable store to the operand stack.</li>
     * </ul>
     */
    public static final int B_LOAD = 164;
    /**
     * D_LOAD Opcode: Represents a load operation that retrieves a double64 value from the local variable store and pushes it onto the operand stack.
     * <p>This opcode is implemented by the {@link DLoadCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Retrieves the index for the local variable from the instruction parameters.</li>
     *     <li>Retrieves the corresponding value from the local variable store of the current method frame.</li>
     *     <li>Pushes the retrieved value onto the operand stack for subsequent operations.</li>
     *     <li>Increments the program counter (PC) to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Loading local variables onto the operand stack for further operations or computations.</li>
     *     <li>Retrieving stored values that are needed for subsequent instructions, such as arithmetic or logic operations.</li>
     *     <li>Preserving the necessary method-local state by pushing relevant values from the local variable store to the operand stack.</li>
     * </ul>
     */
    public static final int D_LOAD = 165;
    /**
     * F_LOAD Opcode: Represents a load operation that retrieves a float32 value from the local variable store and pushes it onto the operand stack.
     * <p>This opcode is implemented by the {@link FLoadCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Retrieves the index for the local variable from the instruction parameters.</li>
     *     <li>Retrieves the corresponding value from the local variable store of the current method frame.</li>
     *     <li>Pushes the retrieved value onto the operand stack for subsequent operations.</li>
     *     <li>Increments the program counter (PC) to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Loading local variables onto the operand stack for further operations or computations.</li>
     *     <li>Retrieving stored values that are needed for subsequent instructions, such as arithmetic or logic operations.</li>
     *     <li>Preserving the necessary method-local state by pushing relevant values from the local variable store to the operand stack.</li>
     * </ul>
     */
    public static final int F_LOAD = 166;

    /**
     * MOV Opcode: Represents a move operation that transfers a value from one local variable to another within the local variable store.
     * <p>This opcode is implemented by the {@link MovCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the source and destination indices from the instruction parameters.</li>
     *     <li>Retrieves the value from the local variable store at the source index.</li>
     *     <li>Stores the retrieved value into the destination index of the local variable store.</li>
     *     <li>Increments the program counter (PC) to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Transferring values between local variables within a method frame.</li>
     *     <li>Preserving computed results by moving them to designated local variable slots.</li>
     *     <li>Facilitating intermediate storage of values needed for later computations.</li>
     * </ul>
     */
    public static final int MOV = 171;
    // endregion
    // region 6. Function Call
    /**
     * CALL Opcode: Represents a function or subroutine call operation that transfers control to a specified function address.
     * <p>This opcode is implemented by the {@link CallCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Validates and extracts the target function address from the instruction parameters.</li>
     *     <li>Creates a new stack frame containing the return address (current PC + 1), the local variable store, and the method context.</li>
     *     <li>Pushes the newly created stack frame onto the call stack to manage the function invocation hierarchy.</li>
     *     <li>Updates the program counter (PC) to the target function address, effectively transferring control to the function.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Invoking functions or subroutines within the virtual machine's execution flow.</li>
     *     <li>Managing method invocations by maintaining a call stack with return addresses and local variable contexts.</li>
     *     <li>Facilitating modular and reusable code execution by enabling jumps to specific function addresses.</li>
     * </ul>
     */
    public static final int CALL = 201;
    /**
     * RET Opcode: Represents a return operation that transfers control back to the calling method by restoring the saved return address.
     * <p>This opcode is implemented by the {@link RetCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Validates that the call stack is not empty to ensure there is a method to return to.</li>
     *     <li>Clears the local variables of the current method using {@link LocalVariableStore#clearVariables()}.</li>
     *     <li>Pops the current stack frame from the call stack to retrieve the saved return address.</li>
     *     <li>Restores the program counter (PC) to the return address, allowing execution to continue from the calling method.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Returning from a function or method to the caller in the virtual machine's execution flow.</li>
     *     <li>Restoring the program's control flow by using the return address saved during the function call.</li>
     *     <li>Cleaning up the local variables of the completed method to maintain memory consistency.</li>
     * </ul>
     */
    public static final int RET = 202;
    /**
     * HALT Opcode: Represents a termination operation that stops the execution of the virtual machine.
     * <p>This opcode is implemented by the {@link HaltCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Outputs the message "Process has ended" to indicate that the program execution has been terminated.</li>
     *     <li>Returns -1 as a signal to the virtual machine to halt execution, preventing any further instructions from being processed.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Terminating program execution explicitly when the program has completed its intended operations.</li>
     *     <li>Serving as the final instruction in a program to indicate normal completion.</li>
     * </ul>
     */
    public static final int HALT = 255;
    // endregion

    /**
     * Default constructor for creating an instance of VMOpCode.
     * This constructor is empty as no specific initialization is required.
     */
    public VMOpCode() {
        // Empty constructor
    }
}
