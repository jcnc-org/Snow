package org.jcnc.snow.vm.commands.system.control.array;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

public class ArrSetHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        Object value = stack.pop();
        Object idxObj = stack.pop();
        Object arrObj = stack.pop();
        int idx = (idxObj instanceof Number n) ? n.intValue()
                : Integer.parseInt(idxObj.toString().trim());

        if (arrObj instanceof java.util.List<?> list) {
            @SuppressWarnings("unchecked")
            java.util.List<Object> mlist = (java.util.List<Object>) list;
            while (mlist.size() < idx) mlist.add(null);
            if (idx == mlist.size()) mlist.add(value);
            else mlist.set(idx, value);
        } else if (arrObj != null && arrObj.getClass().isArray()) {
            java.lang.reflect.Array.set(arrObj, idx, value);
        } else {
            throw new IllegalArgumentException("ARR_SET: not an array/list: " + arrObj);
        }
        stack.push(0);
    }
}