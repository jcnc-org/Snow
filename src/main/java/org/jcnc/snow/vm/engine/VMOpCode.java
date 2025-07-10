package org.jcnc.snow.vm.engine;

import org.jcnc.snow.vm.commands.type.control.byte8.*;
import org.jcnc.snow.vm.commands.type.control.double64.*;
import org.jcnc.snow.vm.commands.type.control.float32.*;
import org.jcnc.snow.vm.commands.type.control.int32.*;
import org.jcnc.snow.vm.commands.type.control.long64.*;
import org.jcnc.snow.vm.commands.type.control.short16.*;
import org.jcnc.snow.vm.commands.type.control.int32.IAndCommand;
import org.jcnc.snow.vm.commands.type.control.int32.IOrCommand;
import org.jcnc.snow.vm.commands.type.control.int32.IXorCommand;
import org.jcnc.snow.vm.commands.type.control.long64.LAndCommand;
import org.jcnc.snow.vm.commands.type.control.long64.LOrCommand;
import org.jcnc.snow.vm.commands.type.control.long64.LXorCommand;
import org.jcnc.snow.vm.commands.flow.control.JumpCommand;
import org.jcnc.snow.vm.commands.flow.control.CallCommand;
import org.jcnc.snow.vm.commands.flow.control.RetCommand;
import org.jcnc.snow.vm.commands.register.control.MovCommand;
import org.jcnc.snow.vm.commands.type.control.byte8.BLoadCommand;
import org.jcnc.snow.vm.commands.type.control.byte8.BStoreCommand;
import org.jcnc.snow.vm.commands.type.control.double64.DLoadCommand;
import org.jcnc.snow.vm.commands.type.control.double64.DStoreCommand;
import org.jcnc.snow.vm.commands.type.control.float32.FLoadCommand;
import org.jcnc.snow.vm.commands.type.control.float32.FStoreCommand;
import org.jcnc.snow.vm.commands.type.control.int32.ILoadCommand;
import org.jcnc.snow.vm.commands.type.control.int32.IStoreCommand;
import org.jcnc.snow.vm.commands.type.control.long64.LLoadCommand;
import org.jcnc.snow.vm.commands.type.control.long64.LStoreCommand;
import org.jcnc.snow.vm.commands.type.control.short16.SLoadCommand;
import org.jcnc.snow.vm.commands.type.control.short16.SStoreCommand;
import org.jcnc.snow.vm.commands.stack.control.DupCommand;
import org.jcnc.snow.vm.commands.stack.control.PopCommand;
import org.jcnc.snow.vm.commands.stack.control.SwapCommand;
import org.jcnc.snow.vm.commands.type.control.byte8.BPushCommand;
import org.jcnc.snow.vm.commands.type.control.double64.DPushCommand;
import org.jcnc.snow.vm.commands.type.control.float32.FPushCommand;
import org.jcnc.snow.vm.commands.type.control.int32.IPushCommand;
import org.jcnc.snow.vm.commands.type.control.long64.LPushCommand;
import org.jcnc.snow.vm.commands.type.control.short16.SPushCommand;
import org.jcnc.snow.vm.commands.type.conversion.*;
import org.jcnc.snow.vm.commands.system.control.HaltCommand;
import org.jcnc.snow.vm.module.LocalVariableStore;

/**
 * VMOpCode defines the compact instruction set for the virtual machine, organized into logical categories.
 * <p>Each opcode represents a specific operation executed by the virtual machine.</p>
 */
public class VMOpCode {

    // region Type Control (0x0000-0x00BF)
    // region Byte8	 (0x0000-0x001F)
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
    public static final int B_ADD = 0x0000;
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
    public static final int B_SUB = 0x0001;
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
    public static final int B_MUL = 0x0002;
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
    public static final int B_DIV = 0x0003;
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
    public static final int B_MOD = 0x0004;
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
    public static final int B_NEG = 0x0005;
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
    public static final int B_INC = 0x0006;
    /**
     * I_AND Opcode: Represents the byte8 bitwise AND operation in the virtual machine.
     * <p>This opcode is implemented by the {@link IAndCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two byte8 values from the operand stack. These values are treated as 32-bit binary representations.</li>
     *     <li>Performs the byte8 bitwise AND operation on the two popped operands. The operation compares corresponding bits of both operands:
     *         <ul>
     *             <li>Each bit in the result is set to <code>1</code> only if the corresponding bits in both operands are also <code>1</code>.</li>
     *             <li>If either of the corresponding bits is <code>0</code>, the resulting bit is <code>0</code>.</li>
     *         </ul>
     *     </li>
     *     <li>Pushes the result of the byte8 bitwise AND operation back onto the operand stack for further processing.</li>
     * </ol>
     *
     * <p>This opcode is essential for low-level bit manipulation tasks.</p>
     */
    public static final int B_AND = 0x0007;
    /**
     * I_OR Opcode: Represents the byte8 bitwise OR operation in the virtual machine.
     * <p>This opcode is implemented by the {@link IOrCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two byte8 values from the operand stack. These values are treated as 32-bit binary representations.</li>
     *     <li>Performs the byte8 bitwise OR operation on the two popped operands. The operation compares corresponding bits of both operands:
     *         <ul>
     *             <li>Each bit in the result is set to <code>1</code> if at least one of the corresponding bits in either operand is <code>1</code>.</li>
     *             <li>If both corresponding bits are <code>0</code>, the resulting bit is <code>0</code>.</li>
     *         </ul>
     *     </li>
     *     <li>Pushes the result of the byte8 bitwise OR operation back onto the operand stack for further processing.</li>
     * </ol>
     *
     * <p>This opcode is essential for low-level bit manipulation tasks.</p>
     */
    public static final int B_OR = 0x0008;
    /**
     * I_XOR Opcode: Represents the byte8 bitwise XOR operation in the virtual machine.
     * <p>This opcode is implemented by the {@link IXorCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two byte8 values from the operand stack. These values are treated as 32-bit binary representations.</li>
     *     <li>Performs the byte8 bitwise XOR (exclusive OR) operation on the two operands:
     *         <ul>
     *             <li>If the corresponding bits are different, the result is <code>1</code>.</li>
     *             <li>If the corresponding bits are the same, the result is <code>0</code>.</li>
     *         </ul>
     *     </li>
     *     <li>Pushes the result of the byte8 bitwise XOR operation back onto the operand stack for further processing.</li>
     * </ol>
     *
     * <p>This opcode is essential for low-level bit manipulation tasks.</p>
     */
    public static final int B_XOR = 0x0009;
    /**
     * B_PUSH Opcode: Represents a stack operation that pushes a byte8 value onto the operand stack.
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
    public static final int B_PUSH = 0x000A;
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
    public static final int B_LOAD = 0x000B;
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
    public static final int B_STORE = 0x000C;
    /**
     * I_CE Opcode: Represents a conditional jump based on byte8 equality.
     * <p>This opcode is implemented by the {@link ICECommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two byte8 values from the operand stack.</li>
     *     <li>Compares the two byte8 for equality.</li>
     *     <li>If the byte8 are equal, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the byte8 are not equal, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on byte8 comparison.</li>
     *     <li>Implementing control flow structures such as if-statements and loops.</li>
     * </ul>
     */
    public static final int B_CE = 0x000D;
    /**
     * I_CNE Opcode: Represents a conditional jump based on byte8 inequality.
     * <p>This opcode is implemented by the {@link ICNECommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two byte8 values from the operand stack.</li>
     *     <li>Compares the two byte8 for inequality.</li>
     *     <li>If the byte8 are not equal, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the byte8 are equal, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on byte8 comparison.</li>
     *     <li>Implementing control flow structures such as conditional loops and if-else statements.</li>
     * </ul>
     */
    public static final int B_CNE = 0x000E;
    /**
     * I_CG Opcode: Represents a conditional jump based on byte8 comparison (greater than).
     * <p>This opcode is implemented by the {@link ICGCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two byte8 values from the operand stack.</li>
     *     <li>Compares the first byte8 with the second to determine if it is greater.</li>
     *     <li>If the first byte8 is greater than the second, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the first byte8 is not greater than the second, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on byte8 comparison.</li>
     *     <li>Implementing control flow structures such as greater-than conditions in loops and conditional statements.</li>
     * </ul>
     */
    public static final int B_CG = 0x000F;
    /**
     * I_CGE Opcode: Represents a conditional jump based on byte8 comparison (greater than or equal to).
     * <p>This opcode is implemented by the {@link ICGECommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two byte8 values from the operand stack.</li>
     *     <li>Compares the first byte8 with the second to determine if it is greater than or equal to the second byte8.</li>
     *     <li>If the first byte8 is greater than or equal to the second, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the first byte8 is less than the second, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on byte8 comparison.</li>
     *     <li>Implementing control flow structures such as loops, conditional statements, and range checks.</li>
     * </ul>
     */
    public static final int B_CGE = 0x0010;
    /**
     * I_CL Opcode: Represents a conditional jump based on byte8 comparison (less than).
     * <p>This opcode is implemented by the {@link ICLCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two byte8 values from the operand stack.</li>
     *     <li>Compares the first byte8 with the second to determine if it is less than the second byte8.</li>
     *     <li>If the first byte8 is less than the second, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the first byte8 is greater than or equal to the second, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on byte8 comparison.</li>
     *     <li>Implementing control flow structures such as loops, conditional statements, and range validations.</li>
     * </ul>
     */
    public static final int B_CL = 0x0011;
    /**
     * B_CLE Opcode: Represents a conditional jump based on byte8 comparison (less than or equal).
     * <p>This opcode is implemented by the {@link ICLECommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two byte8 values from the operand stack.</li>
     *     <li>Compares the first byte8 with the second to determine if it is less than or equal to the second byte8.</li>
     *     <li>If the first byte8 is less than or equal to the second, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the first byte8 is greater than the second, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on byte8 comparison.</li>
     *     <li>Implementing control flow structures such as loops, conditional statements, and boundary checks.</li>
     * </ul>
     */
    public static final int B_CLE = 0x0012;
    // endregion

    // region Short16 (0x0020-0x003F)
    public static final int S_ADD = 0x0020;
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
    public static final int S_SUB = 0x0021;
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
    public static final int S_MUL = 0x0022;
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
    public static final int S_DIV = 0x0023;
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
    public static final int S_MOD = 0x0024;
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
    public static final int S_NEG = 0x0025;
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
    public static final int S_INC = 0x0026;
    /**
     * I_AND Opcode: Represents the short16 bitwise AND operation in the virtual machine.
     * <p>This opcode is implemented by the {@link IAndCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two short16 values from the operand stack. These values are treated as 32-bit binary representations.</li>
     *     <li>Performs the short16 bitwise AND operation on the two popped operands. The operation compares corresponding bits of both operands:
     *         <ul>
     *             <li>Each bit in the result is set to <code>1</code> only if the corresponding bits in both operands are also <code>1</code>.</li>
     *             <li>If either of the corresponding bits is <code>0</code>, the resulting bit is <code>0</code>.</li>
     *         </ul>
     *     </li>
     *     <li>Pushes the result of the short16 bitwise AND operation back onto the operand stack for further processing.</li>
     * </ol>
     *
     * <p>This opcode is essential for low-level bit manipulation tasks.</p>
     */
    public static final int S_AND = 0x0027;
    /**
     * I_OR Opcode: Represents the short16 bitwise OR operation in the virtual machine.
     * <p>This opcode is implemented by the {@link IOrCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two short16 values from the operand stack. These values are treated as 32-bit binary representations.</li>
     *     <li>Performs the short16 bitwise OR operation on the two popped operands. The operation compares corresponding bits of both operands:
     *         <ul>
     *             <li>Each bit in the result is set to <code>1</code> if at least one of the corresponding bits in either operand is <code>1</code>.</li>
     *             <li>If both corresponding bits are <code>0</code>, the resulting bit is <code>0</code>.</li>
     *         </ul>
     *     </li>
     *     <li>Pushes the result of the short16 bitwise OR operation back onto the operand stack for further processing.</li>
     * </ol>
     *
     * <p>This opcode is essential for low-level bit manipulation tasks.</p>
     */
    public static final int S_OR = 0x0028;
    /**
     * I_XOR Opcode: Represents the short16 bitwise XOR operation in the virtual machine.
     * <p>This opcode is implemented by the {@link IXorCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two short16 values from the operand stack. These values are treated as 32-bit binary representations.</li>
     *     <li>Performs the short16 bitwise XOR (exclusive OR) operation on the two operands:
     *         <ul>
     *             <li>If the corresponding bits are different, the result is <code>1</code>.</li>
     *             <li>If the corresponding bits are the same, the result is <code>0</code>.</li>
     *         </ul>
     *     </li>
     *     <li>Pushes the result of the short16 bitwise XOR operation back onto the operand stack for further processing.</li>
     * </ol>
     *
     * <p>This opcode is essential for low-level bit manipulation tasks.</p>
     */
    public static final int S_XOR = 0x0029;
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
    public static final int S_PUSH = 0x002A;
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
    public static final int S_LOAD = 0x002B;
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
    public static final int S_STORE = 0x002C;
    /**
     * I_CE Opcode: Represents a conditional jump based on short16 equality.
     * <p>This opcode is implemented by the {@link ICECommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two short16 values from the operand stack.</li>
     *     <li>Compares the two short16 for equality.</li>
     *     <li>If the short16 are equal, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the short16 are not equal, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on short16 comparison.</li>
     *     <li>Implementing control flow structures such as if-statements and loops.</li>
     * </ul>
     */
    public static final int S_CE = 0x002D;
    /**
     * I_CNE Opcode: Represents a conditional jump based on short16 inequality.
     * <p>This opcode is implemented by the {@link ICNECommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two short16 values from the operand stack.</li>
     *     <li>Compares the two short16 for inequality.</li>
     *     <li>If the short16 are not equal, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the short16 are equal, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on short16 comparison.</li>
     *     <li>Implementing control flow structures such as conditional loops and if-else statements.</li>
     * </ul>
     */
    public static final int S_CNE = 0x002E;
    /**
     * I_CG Opcode: Represents a conditional jump based on short16 comparison (greater than).
     * <p>This opcode is implemented by the {@link ICGCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two short16 values from the operand stack.</li>
     *     <li>Compares the first short16 with the second to determine if it is greater.</li>
     *     <li>If the first short16 is greater than the second, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the first short16 is not greater than the second, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on short16 comparison.</li>
     *     <li>Implementing control flow structures such as greater-than conditions in loops and conditional statements.</li>
     * </ul>
     */
    public static final int S_CG = 0x002F;
    /**
     * I_CGE Opcode: Represents a conditional jump based on short16 comparison (greater than or equal to).
     * <p>This opcode is implemented by the {@link ICGECommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two short16 values from the operand stack.</li>
     *     <li>Compares the first short16 with the second to determine if it is greater than or equal to the second short16.</li>
     *     <li>If the first short16 is greater than or equal to the second, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the first short16 is less than the second, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on short16 comparison.</li>
     *     <li>Implementing control flow structures such as loops, conditional statements, and range checks.</li>
     * </ul>
     */
    public static final int S_CGE = 0x0030;
    /**
     * I_CL Opcode: Represents a conditional jump based on short16 comparison (less than).
     * <p>This opcode is implemented by the {@link ICLCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two short16 values from the operand stack.</li>
     *     <li>Compares the first short16 with the second to determine if it is less than the second short16.</li>
     *     <li>If the first short16 is less than the second, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the first short16 is greater than or equal to the second, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on short16 comparison.</li>
     *     <li>Implementing control flow structures such as loops, conditional statements, and range validations.</li>
     * </ul>
     */
    public static final int S_CL = 0x0031;
    /**
     * S_CLE Opcode: Represents a conditional jump based on short16 comparison (less than or equal).
     * <p>This opcode is implemented by the {@link ICLECommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two short16 values from the operand stack.</li>
     *     <li>Compares the first short16 with the second to determine if it is less than or equal to the second short16.</li>
     *     <li>If the first short16 is less than or equal to the second, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the first short16 is greater than the second, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on short16 comparison.</li>
     *     <li>Implementing control flow structures such as loops, conditional statements, and boundary checks.</li>
     * </ul>
     */
    public static final int S_CLE = 0x0032;
    // endregion

    // region Int32 (0x0040-0x005F)
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
    public static final int I_ADD = 0x0040;
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
    public static final int I_SUB = 0x0041;
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
    public static final int I_MUL = 0x0042;
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
    public static final int I_DIV = 0x0043;
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
    public static final int I_MOD = 0x0044;
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
    public static final int I_NEG = 0x0045;
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
    public static final int I_INC = 0x0046;
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
    public static final int I_AND = 0x0047;
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
    public static final int I_OR = 0x0048;
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
    public static final int I_XOR = 0x0049;
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
    public static final int I_PUSH = 0x004A;
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
    public static final int I_LOAD = 0x004B;
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
    public static final int I_STORE = 0x004C;
    /**
     * I_CE Opcode: Represents a conditional jump based on int32 equality.
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
    public static final int I_CE = 0x004D;
    /**
     * I_CNE Opcode: Represents a conditional jump based on int32 inequality.
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
    public static final int I_CNE = 0x004E;
    /**
     * I_CG Opcode: Represents a conditional jump based on int32 comparison (greater than).
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
    public static final int I_CG = 0x004F;
    /**
     * I_CGE Opcode: Represents a conditional jump based on int32 comparison (greater than or equal to).
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
    public static final int I_CGE = 0x0050;
    /**
     * I_CL Opcode: Represents a conditional jump based on int32 comparison (less than).
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
    public static final int I_CL = 0x0051;
    /**
     * I_CLE Opcode: Represents a conditional jump based on int32 comparison (less than or equal).
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
    public static final int I_CLE = 0x0052;
    // endregion

    // region Long64 (0x0060-0x007F)
    /**
     * L_ADD Opcode: Represents the long64 addition operation in the virtual machine.
     * <p>This opcode is implemented by the {@link LAddCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two long64 values from the operand stack (the first value popped is the second operand, and the second value popped is the first operand).</li>
     *     <li>Performs the addition operation on these two long64.</li>
     *     <li>Pushes the result of the addition back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic long64 addition tasks.</p>
     */
    public static final int L_ADD = 0x0060;
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
    public static final int L_SUB = 0x0061;
    /**
     * L_MUL Opcode: Represents the long64 multiplication operation in the virtual machine.
     * <p>This opcode is implemented by the {@link LMulCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two long64 values from the operand stack (the first value popped is the second operand, and the second value popped is the first operand).</li>
     *     <li>Performs the multiplication operation on these two long64 (i.e., <code>firstOperand * secondOperand</code>).</li>
     *     <li>Pushes the result of the multiplication back onto the operand stack for later instructions to use.</li>
     * </ol>
     *
     * <p>This opcode is a fundamental arithmetic operation within the virtual machine's instruction set,
     * primarily used to handle basic long64 multiplication tasks.</p>
     */
    public static final int L_MUL = 0x0062;
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
    public static final int L_DIV = 0x0063;
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
    public static final int L_MOD = 0x0064;
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
    public static final int L_NEG = 0x0065;
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
    public static final int L_INC = 0x0066;
    /**
     * L_AND Opcode: Represents the long64 bitwise AND operation in the virtual machine.
     * <p>This opcode is implemented by the {@link LAndCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two long64 values from the operand stack. These values are treated as 64-bit binary representations.</li>
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
    public static final int L_AND = 0x0067;
    /**
     * L_OR Opcode: Represents the long64 bitwise OR operation in the virtual machine.
     * <p>This opcode is implemented by the {@link LOrCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two long64 values from the operand stack. These values are treated as 64-bit binary representations.</li>
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
    public static final int L_OR = 0x0068;
    /**
     * L_XOR Opcode: Represents the long64 bitwise XOR operation in the virtual machine.
     * <p>This opcode is implemented by the {@link LXorCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pops the top two long64 values from the operand stack. These values are treated as 64-bit binary representations.</li>
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
    public static final int L_XOR = 0x0069;
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
    public static final int L_PUSH = 0x006A;
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
    public static final int L_LOAD = 0x006B;
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
    public static final int L_STORE = 0x006C;
    /**
     * L_CE Opcode: Represents a conditional jump based on long64 equality.
     * <p>This opcode is implemented by the {@link LCECommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two long64 values from the operand stack.</li>
     *     <li>Compares the two long64 for equality.</li>
     *     <li>If the long64 are equal, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the long64 are not equal, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on long64 comparison.</li>
     *     <li>Implementing control flow structures such as if-statements and loops.</li>
     * </ul>
     */
    public static final int L_CE = 0x006D;
    /**
     * L_CNE Opcode: Represents a conditional jump based on long64 inequality.
     * <p>This opcode is implemented by the {@link LCNECommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two long64 values from the operand stack.</li>
     *     <li>Compares the two long64 for inequality.</li>
     *     <li>If the long64 are not equal, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the long64 are equal, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on long64 comparison.</li>
     *     <li>Implementing control flow structures such as conditional loops and if-else statements.</li>
     * </ul>
     */
    public static final int L_CNE = 0x006E;
    /**
     * L_CG Opcode: Represents a conditional jump based on long64 comparison (greater than).
     * <p>This opcode is implemented by the {@link LCGCommand} class, which defines its specific execution logic.</p>
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
    public static final int L_CG = 0x006F;
    /**
     * L_CGE Opcode: Represents a conditional jump based on long64 comparison (greater than or equal to).
     * <p>This opcode is implemented by the {@link LCGECommand} class, which defines its specific execution logic.</p>
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
    public static final int L_CGE = 0x0070;
    /**
     * L_CL Opcode: Represents a conditional jump based on long64 comparison (less than).
     * <p>This opcode is implemented by the {@link LCLCommand} class, which defines its specific execution logic.</p>
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
    public static final int L_CL = 0x0071;
    /**
     * L_CLE Opcode: Represents a conditional jump based on long64 comparison (less than or equal).
     * <p>This opcode is implemented by the {@link LCLECommand} class, which defines its specific execution logic.</p>
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
    public static final int L_CLE = 0x0072;
    // endregion

    // region Float32 (0x0080-0x009F)
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
    public static final int F_ADD = 0x0080;
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
    public static final int F_SUB = 0x0081;
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
    public static final int F_MUL = 0x0082;
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
    public static final int F_DIV = 0x0083;
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
    public static final int F_MOD = 0x0084;
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
    public static final int F_NEG = 0x0085;
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
    public static final int F_INC = 0x0086;
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
    public static final int F_PUSH = 0x0087;
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
    public static final int F_LOAD = 0x0088;
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
    public static final int F_STORE = 0x0089;
    /**
     * L_CE Opcode: Represents a conditional jump based on float32 equality.
     * <p>This opcode is implemented by the {@link LCECommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two float32 values from the operand stack.</li>
     *     <li>Compares the two float32 for equality.</li>
     *     <li>If the float32 are equal, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the float32 are not equal, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on float32 comparison.</li>
     *     <li>Implementing control flow structures such as if-statements and loops.</li>
     * </ul>
     */
    public static final int F_CE = 0x008A;
    /**
     * L_CNE Opcode: Represents a conditional jump based on float32 inequality.
     * <p>This opcode is implemented by the {@link LCNECommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two float32 values from the operand stack.</li>
     *     <li>Compares the two float32 for inequality.</li>
     *     <li>If the float32 are not equal, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the float32 are equal, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on float32 comparison.</li>
     *     <li>Implementing control flow structures such as conditional loops and if-else statements.</li>
     * </ul>
     */
    public static final int F_CNE = 0x008B;
    /**
     * L_CG Opcode: Represents a conditional jump based on float32 comparison (greater than).
     * <p>This opcode is implemented by the {@link LCGCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two float32 values from the operand stack.</li>
     *     <li>Compares the first float32 with the second to determine if it is greater.</li>
     *     <li>If the first float32 is greater than the second, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the first float32 is not greater than the second, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on float32 comparison.</li>
     *     <li>Implementing control flow structures such as greater-than conditions in loops and conditional statements.</li>
     * </ul>
     */
    public static final int F_CG = 0x008C;
    /**
     * L_CGE Opcode: Represents a conditional jump based on float32 comparison (greater than or equal to).
     * <p>This opcode is implemented by the {@link LCGECommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two float32 values from the operand stack.</li>
     *     <li>Compares the first float32 with the second to determine if it is greater than or equal to the second float32.</li>
     *     <li>If the first float32 is greater than or equal to the second, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the first float32 is less than the second, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on float32 comparison.</li>
     *     <li>Implementing control flow structures such as loops, conditional statements, and range checks.</li>
     * </ul>
     */
    public static final int F_CGE = 0x008D;
    /**
     * L_CL Opcode: Represents a conditional jump based on float32 comparison (less than).
     * <p>This opcode is implemented by the {@link LCLCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two float32 values from the operand stack.</li>
     *     <li>Compares the first float32 with the second to determine if it is less than the second float32.</li>
     *     <li>If the first float32 is less than the second, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the first float32 is greater than or equal to the second, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on float32 comparison.</li>
     *     <li>Implementing control flow structures such as loops, conditional statements, and range validations.</li>
     * </ul>
     */
    public static final int F_CL = 0x008E;
    /**
     * L_CLE Opcode: Represents a conditional jump based on float32 comparison (less than or equal).
     * <p>This opcode is implemented by the {@link LCLECommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two float32 values from the operand stack.</li>
     *     <li>Compares the first float32 with the second to determine if it is less than or equal to the second float32.</li>
     *     <li>If the first float32 is less than or equal to the second, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the first float32 is greater than the second, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on float32 comparison.</li>
     *     <li>Implementing control flow structures such as loops, conditional statements, and boundary checks.</li>
     * </ul>
     */
    public static final int F_CLE = 0x008F;
    // endregion

    // region Double64 (0x00A0-0x00BF)
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
    public static final int D_ADD = 0x00A0;
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
    public static final int D_SUB = 0x00A1;
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
    public static final int D_MUL = 0x00A2;
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
    public static final int D_DIV = 0x00A3;
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
    public static final int D_MOD = 0x00A4;
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
    public static final int D_NEG = 0x00A5;
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
    public static final int D_INC = 0x00A6;
    /**
     * D_PUSH Opcode: Represents a stack operation that pushes a double64 value onto the operand stack.
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
    public static final int D_PUSH = 0x00A7;
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
    public static final int D_LOAD = 0x00A8;
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
    public static final int D_STORE = 0x00A9;
    /**
     * L_CE Opcode: Represents a conditional jump based on double64 equality.
     * <p>This opcode is implemented by the {@link LCECommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two double64 values from the operand stack.</li>
     *     <li>Compares the two double64 for equality.</li>
     *     <li>If the double64 are equal, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the double64 are not equal, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on double64 comparison.</li>
     *     <li>Implementing control flow structures such as if-statements and loops.</li>
     * </ul>
     */
    public static final int D_CE = 0x00AA;
    /**
     * L_CNE Opcode: Represents a conditional jump based on double64 inequality.
     * <p>This opcode is implemented by the {@link LCNECommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two double64 values from the operand stack.</li>
     *     <li>Compares the two double64 for inequality.</li>
     *     <li>If the double64 are not equal, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the double64 are equal, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on double64 comparison.</li>
     *     <li>Implementing control flow structures such as conditional loops and if-else statements.</li>
     * </ul>
     */
    public static final int D_CNE = 0x00AB;
    /**
     * L_CG Opcode: Represents a conditional jump based on double64 comparison (greater than).
     * <p>This opcode is implemented by the {@link LCGCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two double64 values from the operand stack.</li>
     *     <li>Compares the first double64 with the second to determine if it is greater.</li>
     *     <li>If the first double64 is greater than the second, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the first double64 is not greater than the second, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on double64 comparison.</li>
     *     <li>Implementing control flow structures such as greater-than conditions in loops and conditional statements.</li>
     * </ul>
     */
    public static final int D_CG = 0x00AC;
    /**
     * L_CGE Opcode: Represents a conditional jump based on double64 comparison (greater than or equal to).
     * <p>This opcode is implemented by the {@link LCGECommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two double64 values from the operand stack.</li>
     *     <li>Compares the first double64 with the second to determine if it is greater than or equal to the second double64.</li>
     *     <li>If the first double64 is greater than or equal to the second, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the first double64 is less than the second, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on double64 comparison.</li>
     *     <li>Implementing control flow structures such as loops, conditional statements, and range checks.</li>
     * </ul>
     */
    public static final int D_CGE = 0x00AD;
    /**
     * L_CL Opcode: Represents a conditional jump based on double64 comparison (less than).
     * <p>This opcode is implemented by the {@link LCLCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two double64 values from the operand stack.</li>
     *     <li>Compares the first double64 with the second to determine if it is less than the second double64.</li>
     *     <li>If the first double64 is less than the second, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the first double64 is greater than or equal to the second, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on double64 comparison.</li>
     *     <li>Implementing control flow structures such as loops, conditional statements, and range validations.</li>
     * </ul>
     */
    public static final int D_CL = 0x00AE;
    /**
     * L_CLE Opcode: Represents a conditional jump based on double64 comparison (less than or equal).
     * <p>This opcode is implemented by the {@link LCLECommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Parses the target instruction address from the instruction parameters.</li>
     *     <li>Pops two double64 values from the operand stack.</li>
     *     <li>Compares the first double64 with the second to determine if it is less than or equal to the second double64.</li>
     *     <li>If the first double64 is less than or equal to the second, updates the program counter (PC) to the specified target address,
     *         effectively jumping to the target instruction.</li>
     *     <li>If the first double64 is greater than the second, increments the program counter to proceed with the next sequential instruction.</li>
     * </ol>
     *
     * <p>This opcode is commonly used for:</p>
     * <ul>
     *     <li>Conditional branching in virtual machine execution based on double64 comparison.</li>
     *     <li>Implementing control flow structures such as loops, conditional statements, and boundary checks.</li>
     * </ul>
     */
    public static final int D_CLE = 0x00AF;

    // endregion
    // endregion

    // region Type Conversion (0x00C0-0x00DF)
    // region Byte8  (0x00C0-0xC4)

    public static final int B2S = 0x00C0;

    public static final int B2I = 0x00C1;

    public static final int B2L = 0x00C2;

    public static final int B2F = 0x00C3;

    public static final int B2D = 0x00C4;
    // endregion Byte8

    // region Short16  (0x00C5-0xC9)

    public static final int S2B = 0x00C5;

    public static final int S2I = 0x00C6;

    public static final int S2L = 0x00C7;

    public static final int S2F = 0x00C8;

    public static final int S2D = 0x00C9;
    // endregion Short16

    // region Int32  (0x00CA-0xCE)
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
    public static final int I2B = 0x00CA;

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
    public static final int I2S = 0x00CB;

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
    public static final int I2L = 0x00CC;
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
    public static final int I2F = 0x00CD;
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
    public static final int I2D = 0x00CE;
    // endregion Int32

    // region Long64  (0x00CF-0xD3)
    /**
     * L2B Opcode: Represents the type conversion operation from long64 to byte8 in the virtual machine.
     * <p>This opcode is implemented by the {@link L2BCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pop the top long64 value from the operand stack.</li>
     *     <li>Convert the long64 value to a byte8 value (this may involve truncation).</li>
     *     <li>Push the converted byte8 value back onto the operand stack for subsequent operations.</li>
     * </ol>
     *
     * <p>This opcode is used to narrow a long64 value to a byte8 type, suitable when a smaller numeric type is required.</p>
     */
    public static final int L2B = 0x00CF;
    /**
     * L2S Opcode: Represents the type conversion operation from long64 to short16 in the virtual machine.
     * <p>This opcode is implemented by the {@link L2SCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pop the top long64 value from the operand stack.</li>
     *     <li>Convert the long64 value to a short16 value (this may involve truncation).</li>
     *     <li>Push the converted short16 value back onto the operand stack for subsequent operations.</li>
     * </ol>
     *
     * <p>This opcode is used to narrow a long64 value to a short16 type, suitable when a smaller numeric type is required.</p>
     */
    public static final int L2S = 0x00D0;
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
    public static final int L2I = 0x00D1;
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
    public static final int L2F = 0x00D2;
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
    public static final int L2D = 0x00D3;
    // endregion Long64

    // region Float32  (0x00D4-0xD8)
    /**
     * F2B Opcode: Represents the type conversion operation from float32 to byte8 in the virtual machine.
     * <p>This opcode is implemented by the {@link F2BCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pop the top float32 value from the operand stack.</li>
     *     <li>Convert the float32 value to a byte8 value (this may involve truncation).</li>
     *     <li>Push the converted byte8 value back onto the operand stack for subsequent operations.</li>
     * </ol>
     *
     * <p>This opcode is used to convert a float32 value to a byte8 type for further integer-based operations or comparisons.</p>
     */
    public static final int F2B = 0x00D4;
    /**
     * F2S Opcode: Represents the type conversion operation from float32 to short16 in the virtual machine.
     * <p>This opcode is implemented by the {@link F2SCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pop the top float32 value from the operand stack.</li>
     *     <li>Convert the float32 value to a short16 value (this may involve truncation).</li>
     *     <li>Push the converted short16 value back onto the operand stack for subsequent operations.</li>
     * </ol>
     *
     * <p>This opcode is used to convert a float32 value to a short16 type for further integer-based operations or comparisons.</p>
     */
    public static final int F2S = 0x00D5;
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
    public static final int F2I = 0x00D6;
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
    public static final int F2L = 0x00D7;
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
    public static final int F2D = 0x00D8;
    // endregion Float32

    // region Double64  (0x00D9-0xDD)
    /**
     * D2B Opcode: Represents the type conversion operation from double64 to byte8 in the virtual machine.
     * <p>This opcode is implemented by the {@link D2BCommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pop the top double64 value from the operand stack.</li>
     *     <li>Convert the double64 value to a byte8 value (this may involve truncation).</li>
     *     <li>Push the converted byte8 value back onto the operand stack for subsequent operations.</li>
     * </ol>
     *
     * <p>This opcode is used to narrow a double64 value to a byte8 type for further integer-based processing.</p>
     */
    public static final int D2B = 0x00D9;
    /**
     * D2I Opcode: Represents the type conversion operation from double64 to short16 in the virtual machine.
     * <p>This opcode is implemented by the {@link D2ICommand} class, which defines its specific execution logic.</p>
     *
     * <p>Execution Steps:</p>
     * <ol>
     *     <li>Pop the top double64 value from the operand stack.</li>
     *     <li>Convert the double64 value to a short16 value (this may involve truncation).</li>
     *     <li>Push the converted short16 value back onto the operand stack for subsequent operations.</li>
     * </ol>
     *
     * <p>This opcode is used to narrow a double64 value to a short16 type for further integer-based processing.</p>
     */
    public static final int D2S = 0x00DA;
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
    public static final int D2I = 0x00DB;
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
    public static final int D2L = 0x00DC;
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
    public static final int D2F = 0x00DD;
    // endregion Double64
    // endregion Conversion

    // region Stack Control (0x0100-0x01FF)
    /**
     * POP Opcode: Represents a stack operation that removes the top element from the operand stack.
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
    public static final int POP = 0x0100;
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
    public static final int DUP = 0x0101;
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
    public static final int SWAP = 0x0102;
    // endregion

    // region Flow Control (0x0200-0x02FF)
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
    public static final int JUMP = 0x0200;
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
    public static final int CALL = 0x0201;
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
    public static final int RET = 0x0202;
    // endregion

    // region Register Control (0x0300-0x03FF)
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
    public static final int MOV = 0x0300;
    // endregion

    // region  System Control (0x0400-0x04FF)
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
    public static final int HALT = 0x0400;
    public static final int SYSCALL = 0x0401;
    public static final int DEBUG_TRAP = 0x0402;
    // endregion

    /**
     * Default constructor for creating an instance of VMOpCode.
     * This constructor is empty as no specific initialization is required.
     */
    public VMOpCode() {
        // Empty constructor
    }
}