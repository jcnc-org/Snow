package org.jcnc.snow.vm.commands.system.control.array;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

public class ArrGetHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        Object idxObj = stack.pop();
        Object arrObj = stack.pop();
        int idx = (idxObj instanceof Number n) ? n.intValue()
                : Integer.parseInt(idxObj.toString().trim());

        Object elem;
        if (arrObj instanceof java.util.List<?> list) {
            elem = list.get(idx);
        } else if (arrObj != null && arrObj.getClass().isArray()) {
            elem = java.lang.reflect.Array.get(arrObj, idx);
        } else {
            throw new IllegalArgumentException("ARR_GET: not an array/list: " + arrObj);
        }

        if (elem instanceof Number n) stack.push(n);
        else if (elem instanceof Boolean b) stack.push(b ? 1 : 0);
        else stack.push(elem);
    }
}
