package org.jcnc.snow.vm.factories;

import org.jcnc.snow.vm.commands.arithmetic.conversion.*;
import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.commands.arithmetic.byte8.*;
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
import org.jcnc.snow.vm.engine.VMOpCode;

import java.util.Optional;

/**
 * The CommandFactory class is responsible
 * for getting the corresponding instruction instance based on the operation code.
 * <p>This class uses an array for fast, constant-time access to corresponding command instances.</p>
 */
public class CommandFactory {
    private static final Command[] COMMANDS = new Command[1000]; // Adjust, according to your VMOpCode range

    static {
        // Initialize the array with corresponding commands based on opCode values

        // 1   Arithmetic Operations (1–80)
        // 1.1 int32 (1-10)
        COMMANDS[VMOpCode.I_ADD] = new IAddCommand();      // 1
        COMMANDS[VMOpCode.I_SUB] = new ISubCommand();      // 2
        COMMANDS[VMOpCode.I_MUL] = new IMulCommand();      // 3
        COMMANDS[VMOpCode.I_DIV] = new IDivCommand();      // 4
        COMMANDS[VMOpCode.I_MOD] = new IModCommand();      // 5
        COMMANDS[VMOpCode.I_INC] = new IIncCommand();      // 6
        COMMANDS[VMOpCode.I_NEG] = new INegCommand();      // 7
        // 1.2 long64 (11-20)
        COMMANDS[VMOpCode.L_ADD] = new LAddCommand();      // 11
        COMMANDS[VMOpCode.L_SUB] = new LSubCommand();      // 12
        COMMANDS[VMOpCode.L_MUL] = new LMulCommand();      // 13
        COMMANDS[VMOpCode.L_DIV] = new LDivCommand();      // 14
        COMMANDS[VMOpCode.L_MOD] = new LModCommand();      // 15
        COMMANDS[VMOpCode.L_INC] = new LIncCommand();      // 16
        COMMANDS[VMOpCode.L_NEG] = new LNegCommand();      // 17
        // 1.3 short16 (21-30)
        COMMANDS[VMOpCode.S_ADD] = new SAddCommand();      // 21
        COMMANDS[VMOpCode.S_SUB] = new SSubCommand();      // 22
        COMMANDS[VMOpCode.S_MUL] = new SMulCommand();      // 23
        COMMANDS[VMOpCode.S_DIV] = new SDivCommand();      // 24
        COMMANDS[VMOpCode.S_MOD] = new SModCommand();      // 25
        COMMANDS[VMOpCode.S_INC] = new SIncCommand();      // 26
        COMMANDS[VMOpCode.S_NEG] = new SNegCommand();      // 27
        // 1.4 byte8 (31-40)
        COMMANDS[VMOpCode.B_ADD] = new BAddCommand();      // 31
        COMMANDS[VMOpCode.B_SUB] = new BSubCommand();      // 32
        COMMANDS[VMOpCode.B_MUL] = new BMulCommand();      // 33
        COMMANDS[VMOpCode.B_DIV] = new BDivCommand();      // 34
        COMMANDS[VMOpCode.B_MOD] = new BModCommand();      // 35
        COMMANDS[VMOpCode.B_INC] = new BIncCommand();      // 36
        COMMANDS[VMOpCode.B_NEG] = new BNegCommand();      // 37
        // 1.5 double64 (41-50)
        COMMANDS[VMOpCode.D_ADD] = new DAddCommand();      // 41
        COMMANDS[VMOpCode.D_SUB] = new DSubCommand();      // 42
        COMMANDS[VMOpCode.D_MUL] = new DMulCommand();      // 43
        COMMANDS[VMOpCode.D_DIV] = new DDivCommand();      // 44
        COMMANDS[VMOpCode.D_MOD] = new DModCommand();      // 45
        COMMANDS[VMOpCode.D_INC] = new DIncCommand();      // 46
        COMMANDS[VMOpCode.D_NEG] = new DNegCommand();      // 47
        // 1.6 float32 (51-60)
        COMMANDS[VMOpCode.F_ADD] = new FAddCommand();      // 51
        COMMANDS[VMOpCode.F_SUB] = new FSubCommand();      // 52
        COMMANDS[VMOpCode.F_MUL] = new FMulCommand();      // 53
        COMMANDS[VMOpCode.F_DIV] = new FDivCommand();      // 54
        COMMANDS[VMOpCode.F_MOD] = new FModCommand();      // 55
        COMMANDS[VMOpCode.F_INC] = new FIncCommand();      // 56
        COMMANDS[VMOpCode.F_NEG] = new FNegCommand();      // 57

        // 1.7 Type Conversion (61-80)
        COMMANDS[VMOpCode.I2L] = new I2LCommand();      // 61  int -> long
        COMMANDS[VMOpCode.I2S] = new I2SCommand();      // 62  int -> short
        COMMANDS[VMOpCode.I2B] = new I2BCommand();      // 63  int -> byte
        COMMANDS[VMOpCode.I2D] = new I2DCommand();      // 64  int -> double
        COMMANDS[VMOpCode.I2F] = new I2FCommand();      // 65  int -> float

        COMMANDS[VMOpCode.L2I] = new L2ICommand();      // 66  long -> int
        COMMANDS[VMOpCode.L2D] = new L2DCommand();      // 67  long -> double
        COMMANDS[VMOpCode.L2F] = new L2FCommand();      // 68  long -> float

        COMMANDS[VMOpCode.F2I] = new F2ICommand();      // 69  float -> int
        COMMANDS[VMOpCode.F2L] = new F2LCommand();      // 70  float -> long
        COMMANDS[VMOpCode.F2D] = new F2DCommand();      // 71  float -> double

        COMMANDS[VMOpCode.D2I] = new D2ICommand();      // 72  double -> int
        COMMANDS[VMOpCode.D2L] = new D2LCommand();      // 73  double -> long
        COMMANDS[VMOpCode.D2F] = new D2FCommand();      // 74  double -> float

        COMMANDS[VMOpCode.S2I] = new S2ICommand();      // 75  short -> int

        COMMANDS[VMOpCode.B2I] = new B2ICommand();      // 76  byte -> int

        // 1.8 Other (77-80)

        // 2.  Bitwise Operations (81–90)
        // 2.1 int32 (81-85)
        COMMANDS[VMOpCode.I_AND] = new IAndCommand();      // 81
        COMMANDS[VMOpCode.I_OR] = new IOrCommand();        // 82
        COMMANDS[VMOpCode.I_XOR] = new IXorCommand();      // 83
        // 2.2 Long64 (86-90)
        COMMANDS[VMOpCode.L_AND] = new LAndCommand();      // 86
        COMMANDS[VMOpCode.L_OR] = new LOrCommand();        // 87
        COMMANDS[VMOpCode.L_XOR] = new LXorCommand();      // 88

        // 3. Control Flow Operations (91–110)
        COMMANDS[VMOpCode.JUMP] = new JumpCommand();       // 91
        COMMANDS[VMOpCode.IC_E] = new ICECommand();        // 92
        COMMANDS[VMOpCode.IC_NE] = new ICNECommand();      // 93
        COMMANDS[VMOpCode.IC_G] = new ICGCommand();        // 94
        COMMANDS[VMOpCode.IC_GE] = new ICGECommand();      // 95
        COMMANDS[VMOpCode.IC_L] = new ICLCommand();        // 96
        COMMANDS[VMOpCode.IC_LE] = new ICLECommand();      // 97

        // 4.  Stack Operations (111–150)
        // 4.1 PUSH (111-120)
        COMMANDS[VMOpCode.I_PUSH] = new IPushCommand();   // 111
        COMMANDS[VMOpCode.L_PUSH] = new LPushCommand();   // 112
        COMMANDS[VMOpCode.S_PUSH] = new SPushCommand();   // 113
        COMMANDS[VMOpCode.B_PUSH] = new BPushCommand();   // 114
        COMMANDS[VMOpCode.D_PUSH] = new DPushCommand();   // 115
        COMMANDS[VMOpCode.F_PUSH] = new FPushCommand();   // 116
        // 4.2 POP (121-125)
        COMMANDS[VMOpCode.POP] = new PopCommand();        // 121
        // 4.3 DUP (126-130)
        COMMANDS[VMOpCode.DUP] = new DupCommand();        // 126
        // 4.4 SWAP (131-135)
        COMMANDS[VMOpCode.SWAP] = new SwapCommand();      // 131

        // 4.5 Other (136-150)

        // 5.  Memory Operations (151–200)
        // 5.1 STORE (151-160)
        COMMANDS[VMOpCode.I_STORE] = new IStoreCommand();   // 151
        COMMANDS[VMOpCode.L_STORE] = new LStoreCommand();   // 152
        COMMANDS[VMOpCode.S_STORE] = new SStoreCommand();   // 153
        COMMANDS[VMOpCode.B_STORE] = new BStoreCommand();   // 154
        COMMANDS[VMOpCode.D_STORE] = new DStoreCommand();   // 155
        COMMANDS[VMOpCode.F_STORE] = new FStoreCommand();   // 156
        // 5.2 LOAD (161-170)
        COMMANDS[VMOpCode.I_LOAD] = new ILoadCommand();     // 161
        COMMANDS[VMOpCode.L_LOAD] = new LLoadCommand();     // 162
        COMMANDS[VMOpCode.S_LOAD] = new SLoadCommand();     // 163
        COMMANDS[VMOpCode.B_LOAD] = new BLoadCommand();     // 164
        COMMANDS[VMOpCode.D_LOAD] = new DLoadCommand();     // 165
        COMMANDS[VMOpCode.F_LOAD] = new FLoadCommand();     // 166

        // 5.3 MOV (171-176)
        COMMANDS[VMOpCode.MOV] = new MovCommand();          // 171

        // 5.4 Other (176-200)

        // 6. Function Operations (201–205)
        COMMANDS[VMOpCode.CALL] = new CallCommand();        // 201
        COMMANDS[VMOpCode.RET] = new RetCommand();          // 202

        // 7. Virtual Machine Operations(241-255)
        COMMANDS[VMOpCode.HALT] = new HaltCommand();        // 255
    }

    /**
     * Default constructor for creating an instance of CommandFactory.
     * This constructor is empty as no specific initialization is required.
     */
    public CommandFactory() {
        // Empty constructor
    }

    /**
     * Retrieves the corresponding command instance based on the operation code.
     * <p>This method looks up the given operation code and returns the corresponding command instance.</p>
     *
     * @param opCode The operation code (instruction code)
     * @return An Optional containing the command object. If a command exists for the operation code, it returns the command instance; otherwise, an empty Optional is returned.
     */
    public static Optional<Command> getInstruction(int opCode) {
        if (opCode >= 0 && opCode < COMMANDS.length) {
            Command command = COMMANDS[opCode];
            return Optional.ofNullable(command);  // Return the command if it's present, otherwise return empty Optional
        }
        // Return empty Optional if opCode is out of range
        return Optional.empty();
    }
}
