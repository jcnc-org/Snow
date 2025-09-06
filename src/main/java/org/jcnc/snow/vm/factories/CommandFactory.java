package org.jcnc.snow.vm.factories;

import org.jcnc.snow.vm.commands.flow.control.CallCommand;
import org.jcnc.snow.vm.commands.flow.control.JumpCommand;
import org.jcnc.snow.vm.commands.flow.control.RetCommand;
import org.jcnc.snow.vm.commands.ref.control.RLoadCommand;
import org.jcnc.snow.vm.commands.ref.control.RPushCommand;
import org.jcnc.snow.vm.commands.ref.control.RStoreCommand;
import org.jcnc.snow.vm.commands.register.control.MovCommand;
import org.jcnc.snow.vm.commands.stack.control.DupCommand;
import org.jcnc.snow.vm.commands.stack.control.PopCommand;
import org.jcnc.snow.vm.commands.stack.control.SwapCommand;
import org.jcnc.snow.vm.commands.system.control.HaltCommand;
import org.jcnc.snow.vm.commands.system.control.SyscallCommand;
import org.jcnc.snow.vm.commands.type.control.byte8.*;
import org.jcnc.snow.vm.commands.type.control.double64.*;
import org.jcnc.snow.vm.commands.type.control.float32.*;
import org.jcnc.snow.vm.commands.type.control.int32.*;
import org.jcnc.snow.vm.commands.type.control.long64.*;
import org.jcnc.snow.vm.commands.type.control.short16.*;
import org.jcnc.snow.vm.commands.type.conversion.*;
import org.jcnc.snow.vm.engine.VMOpCode;
import org.jcnc.snow.vm.interfaces.Command;

import java.util.Optional;

/**
 * The CommandFactory class is responsible
 * for getting the corresponding instruction instance based on the operation code.
 * <p>This class uses an array for fast, constant-time access to corresponding command instances.</p>
 */
public class CommandFactory {
    /**
     * Complete command table. 0x0000 – 0x04FF (inclusive).
     */
    private static final Command[] COMMANDS = new Command[0x0500];

    static {


        // region Type Control (0x0000-0x00BF)
        // region Byte8	 (0x0000-0x001F)
        COMMANDS[VMOpCode.B_ADD] = new BAddCommand();
        COMMANDS[VMOpCode.B_SUB] = new BSubCommand();
        COMMANDS[VMOpCode.B_MUL] = new BMulCommand();
        COMMANDS[VMOpCode.B_DIV] = new BDivCommand();
        COMMANDS[VMOpCode.B_MOD] = new BModCommand();
        COMMANDS[VMOpCode.B_NEG] = new BNegCommand();
        COMMANDS[VMOpCode.B_INC] = new BIncCommand();

        COMMANDS[VMOpCode.B_AND] = new BAndCommand();
        COMMANDS[VMOpCode.B_OR] = new BOrCommand();
        COMMANDS[VMOpCode.B_XOR] = new BXorCommand();

        COMMANDS[VMOpCode.B_PUSH] = new BPushCommand();
        COMMANDS[VMOpCode.B_LOAD] = new BLoadCommand();
        COMMANDS[VMOpCode.B_STORE] = new BStoreCommand();

        COMMANDS[VMOpCode.B_CE] = new BCECommand();
        COMMANDS[VMOpCode.B_CNE] = new BCNECommand();
        COMMANDS[VMOpCode.B_CG] = new BCGCommand();
        COMMANDS[VMOpCode.B_CGE] = new BCGECommand();
        COMMANDS[VMOpCode.B_CL] = new BCLCommand();
        COMMANDS[VMOpCode.B_CLE] = new BCLECommand();

        // endregion

        // region Short16 (0x0020-0x003F)
        COMMANDS[VMOpCode.S_ADD] = new SAddCommand();
        COMMANDS[VMOpCode.S_SUB] = new SSubCommand();
        COMMANDS[VMOpCode.S_MUL] = new SMulCommand();
        COMMANDS[VMOpCode.S_DIV] = new SDivCommand();
        COMMANDS[VMOpCode.S_MOD] = new SModCommand();
        COMMANDS[VMOpCode.S_NEG] = new SNegCommand();
        COMMANDS[VMOpCode.S_INC] = new SIncCommand();

        COMMANDS[VMOpCode.S_AND] = new SAndCommand();
        COMMANDS[VMOpCode.S_OR] = new SOrCommand();
        COMMANDS[VMOpCode.S_XOR] = new SXorCommand();

        COMMANDS[VMOpCode.S_PUSH] = new SPushCommand();
        COMMANDS[VMOpCode.S_LOAD] = new SLoadCommand();
        COMMANDS[VMOpCode.S_STORE] = new SStoreCommand();

        COMMANDS[VMOpCode.S_CE] = new SCECommand();
        COMMANDS[VMOpCode.S_CNE] = new SCNECommand();
        COMMANDS[VMOpCode.S_CG] = new SCGCommand();
        COMMANDS[VMOpCode.S_CGE] = new SCGECommand();
        COMMANDS[VMOpCode.S_CL] = new SCLCommand();
        COMMANDS[VMOpCode.S_CLE] = new SCLECommand();
        // endregion

        // region Int32 (0x0040-0x005F)
        COMMANDS[VMOpCode.I_ADD] = new IAddCommand();
        COMMANDS[VMOpCode.I_SUB] = new ISubCommand();
        COMMANDS[VMOpCode.I_MUL] = new IMulCommand();
        COMMANDS[VMOpCode.I_DIV] = new IDivCommand();
        COMMANDS[VMOpCode.I_MOD] = new IModCommand();
        COMMANDS[VMOpCode.I_NEG] = new INegCommand();
        COMMANDS[VMOpCode.I_INC] = new IIncCommand();

        COMMANDS[VMOpCode.I_AND] = new IAndCommand();
        COMMANDS[VMOpCode.I_OR] = new IOrCommand();
        COMMANDS[VMOpCode.I_XOR] = new IXorCommand();

        COMMANDS[VMOpCode.I_PUSH] = new IPushCommand();
        COMMANDS[VMOpCode.I_LOAD] = new ILoadCommand();
        COMMANDS[VMOpCode.I_STORE] = new IStoreCommand();

        COMMANDS[VMOpCode.I_CE] = new ICECommand();
        COMMANDS[VMOpCode.I_CNE] = new ICNECommand();
        COMMANDS[VMOpCode.I_CG] = new ICGCommand();
        COMMANDS[VMOpCode.I_CGE] = new ICGECommand();
        COMMANDS[VMOpCode.I_CL] = new ICLCommand();
        COMMANDS[VMOpCode.I_CLE] = new ICLECommand();
        // endregion

        // region Long64 (0x0060-0x007F)
        COMMANDS[VMOpCode.L_ADD] = new LAddCommand();
        COMMANDS[VMOpCode.L_SUB] = new LSubCommand();
        COMMANDS[VMOpCode.L_MUL] = new LMulCommand();
        COMMANDS[VMOpCode.L_DIV] = new LDivCommand();
        COMMANDS[VMOpCode.L_MOD] = new LModCommand();
        COMMANDS[VMOpCode.L_NEG] = new LNegCommand();
        COMMANDS[VMOpCode.L_INC] = new LIncCommand();

        COMMANDS[VMOpCode.L_AND] = new LAndCommand();
        COMMANDS[VMOpCode.L_OR] = new LOrCommand();
        COMMANDS[VMOpCode.L_XOR] = new LXorCommand();

        COMMANDS[VMOpCode.L_PUSH] = new LPushCommand();
        COMMANDS[VMOpCode.L_LOAD] = new LLoadCommand();
        COMMANDS[VMOpCode.L_STORE] = new LStoreCommand();

        COMMANDS[VMOpCode.L_CE] = new LCECommand();
        COMMANDS[VMOpCode.L_CNE] = new LCNECommand();
        COMMANDS[VMOpCode.L_CG] = new LCGCommand();
        COMMANDS[VMOpCode.L_CGE] = new LCGECommand();
        COMMANDS[VMOpCode.L_CL] = new LCLCommand();
        COMMANDS[VMOpCode.L_CLE] = new LCLECommand();
        // endregion

        // region Float32 (0x0080-0x009F)
        COMMANDS[VMOpCode.F_ADD] = new FAddCommand();
        COMMANDS[VMOpCode.F_SUB] = new FSubCommand();
        COMMANDS[VMOpCode.F_MUL] = new FMulCommand();
        COMMANDS[VMOpCode.F_DIV] = new FDivCommand();
        COMMANDS[VMOpCode.F_MOD] = new FModCommand();
        COMMANDS[VMOpCode.F_NEG] = new FNegCommand();
        COMMANDS[VMOpCode.F_INC] = new FIncCommand();

        COMMANDS[VMOpCode.F_PUSH] = new FPushCommand();
        COMMANDS[VMOpCode.F_LOAD] = new FLoadCommand();
        COMMANDS[VMOpCode.F_STORE] = new FStoreCommand();

        COMMANDS[VMOpCode.F_CE] = new FCECommand();
        COMMANDS[VMOpCode.F_CNE] = new FCNECommand();
        COMMANDS[VMOpCode.F_CG] = new FCGCommand();
        COMMANDS[VMOpCode.F_CGE] = new FCGECommand();
        COMMANDS[VMOpCode.F_CL] = new FCLCommand();
        COMMANDS[VMOpCode.F_CLE] = new FCLECommand();
        // endregion

        // region Double64 (0x00A0-0x00BF)
        COMMANDS[VMOpCode.D_ADD] = new DAddCommand();
        COMMANDS[VMOpCode.D_SUB] = new DSubCommand();
        COMMANDS[VMOpCode.D_MUL] = new DMulCommand();
        COMMANDS[VMOpCode.D_DIV] = new DDivCommand();
        COMMANDS[VMOpCode.D_MOD] = new DModCommand();
        COMMANDS[VMOpCode.D_NEG] = new DNegCommand();
        COMMANDS[VMOpCode.D_INC] = new DIncCommand();

        COMMANDS[VMOpCode.D_PUSH] = new DPushCommand();
        COMMANDS[VMOpCode.D_LOAD] = new DLoadCommand();
        COMMANDS[VMOpCode.D_STORE] = new DStoreCommand();

        COMMANDS[VMOpCode.D_CE] = new DCECommand();
        COMMANDS[VMOpCode.D_CNE] = new DCNECommand();
        COMMANDS[VMOpCode.D_CG] = new DCGCommand();
        COMMANDS[VMOpCode.D_CGE] = new DCGECommand();
        COMMANDS[VMOpCode.D_CL] = new DCLCommand();
        COMMANDS[VMOpCode.D_CLE] = new DCLECommand();
        // endregion

        // endregion

        // region Type Conversion (0x00C0-0x00DF)
        COMMANDS[VMOpCode.B2S] = new B2SCommand();
        COMMANDS[VMOpCode.B2I] = new B2ICommand();
        COMMANDS[VMOpCode.B2L] = new B2LCommand();
        COMMANDS[VMOpCode.B2F] = new B2FCommand();
        COMMANDS[VMOpCode.B2D] = new B2DCommand();

        COMMANDS[VMOpCode.S2B] = new S2BCommand();
        COMMANDS[VMOpCode.S2I] = new S2ICommand();
        COMMANDS[VMOpCode.S2L] = new S2LCommand();
        COMMANDS[VMOpCode.S2F] = new S2FCommand();
        COMMANDS[VMOpCode.S2D] = new S2DCommand();

        COMMANDS[VMOpCode.I2B] = new I2BCommand();
        COMMANDS[VMOpCode.I2S] = new I2SCommand();
        COMMANDS[VMOpCode.I2L] = new I2LCommand();
        COMMANDS[VMOpCode.I2F] = new I2FCommand();
        COMMANDS[VMOpCode.I2D] = new I2DCommand();

        COMMANDS[VMOpCode.L2B] = new L2BCommand();
        COMMANDS[VMOpCode.L2S] = new L2SCommand();
        COMMANDS[VMOpCode.L2I] = new L2ICommand();
        COMMANDS[VMOpCode.L2F] = new L2FCommand();
        COMMANDS[VMOpCode.L2D] = new L2DCommand();

        COMMANDS[VMOpCode.F2B] = new F2BCommand();
        COMMANDS[VMOpCode.F2S] = new F2SCommand();
        COMMANDS[VMOpCode.F2I] = new F2ICommand();
        COMMANDS[VMOpCode.F2L] = new F2LCommand();
        COMMANDS[VMOpCode.F2D] = new F2DCommand();

        COMMANDS[VMOpCode.D2B] = new D2BCommand();
        COMMANDS[VMOpCode.D2S] = new D2SCommand();
        COMMANDS[VMOpCode.D2I] = new D2ICommand();
        COMMANDS[VMOpCode.D2L] = new D2LCommand();
        COMMANDS[VMOpCode.D2F] = new D2FCommand();
        // endregion

        // region Reference Control  (0x00E0-0x00EF)
        COMMANDS[VMOpCode.R_PUSH] = new RPushCommand();
        COMMANDS[VMOpCode.R_LOAD] = new RLoadCommand();
        COMMANDS[VMOpCode.R_STORE] = new RStoreCommand();
        // endregion

        // region Stack Control (0x0100-0x01FF)
        COMMANDS[VMOpCode.POP] = new PopCommand();
        COMMANDS[VMOpCode.DUP] = new DupCommand();
        COMMANDS[VMOpCode.SWAP] = new SwapCommand();
        // endregion

        // region Flow Control (0x0200-0x02FF)
        COMMANDS[VMOpCode.JUMP] = new JumpCommand();
        COMMANDS[VMOpCode.CALL] = new CallCommand();
        COMMANDS[VMOpCode.RET] = new RetCommand();
        // endregion

        // region Register Control (0x0300-0x03FF)
        COMMANDS[VMOpCode.MOV] = new MovCommand();
        // endregion

        // region  System Control (0x0400-0x04FF)
        COMMANDS[VMOpCode.HALT] = new HaltCommand();
        COMMANDS[VMOpCode.SYSCALL] = new SyscallCommand();
//        COMMANDS[VMOpCode.DEBUG_TRAP]  = new DebugTrapCommand();
        // endregion

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
