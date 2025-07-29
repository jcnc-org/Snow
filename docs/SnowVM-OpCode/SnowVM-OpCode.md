# SnowVM-OpCode

## Type Control (0x0000-0x00BF)

### Byte8	 (0x0000-0x001F)
| 指令名      | 十六进制   | 说明             |
|----------|--------|----------------|
| B\_ADD   | 0x0000 | byte8 加法       |
| B\_SUB   | 0x0001 | byte8 减法       |
| B\_MUL   | 0x0002 | byte8 乘法       |
| B\_DIV   | 0x0003 | byte8 除法       |
| B\_MOD   | 0x0004 | byte8 取余       |
| B\_NEG   | 0x0005 | byte8 取负       |
| B\_INC   | 0x0006 | byte8 自增       |
| B\_AND   | 0x0007 | byte8 按位与      |
| B\_OR    | 0x0008 | byte8 按位或      |
| B\_XOR   | 0x0009 | byte8 按位异或     |
| B\_PUSH  | 0x000A | byte8 入栈       |
| B\_LOAD  | 0x000B | byte8 本地变量加载   |
| B\_STORE | 0x000C | byte8 本地变量存储   |
| B\_CE    | 0x000D | byte8 等于条件判断   |
| B\_CNE   | 0x000E | byte8 不等于条件判断  |
| B\_CG    | 0x000F | byte8 大于条件判断   |
| B\_CGE   | 0x0010 | byte8 大于等于条件判断 |
| B\_CL    | 0x0011 | byte8 小于条件判断   |
| B\_CLE   | 0x0012 | byte8 小于等于条件判断 |

---

### Short16 (0x0020-0x003F)

| 指令名      | 十六进制   | 说明               |
|----------|--------|------------------|
| S\_ADD   | 0x0020 | short16 加法       |
| S\_SUB   | 0x0021 | short16 减法       |
| S\_MUL   | 0x0022 | short16 乘法       |
| S\_DIV   | 0x0023 | short16 除法       |
| S\_MOD   | 0x0024 | short16 取余       |
| S\_NEG   | 0x0025 | short16 取负       |
| S\_INC   | 0x0026 | short16 自增       |
| S\_AND   | 0x0027 | short16 按位与      |
| S\_OR    | 0x0028 | short16 按位或      |
| S\_XOR   | 0x0029 | short16 按位异或     |
| S\_PUSH  | 0x002A | short16 入栈       |
| S\_LOAD  | 0x002B | short16 本地变量加载   |
| S\_STORE | 0x002C | short16 本地变量存储   |
| S\_CE    | 0x002D | short16 等于条件判断   |
| S\_CNE   | 0x002E | short16 不等于条件判断  |
| S\_CG    | 0x002F | short16 大于条件判断   |
| S\_CGE   | 0x0030 | short16 大于等于条件判断 |
| S\_CL    | 0x0031 | short16 小于条件判断   |
| S\_CLE   | 0x0032 | short16 小于等于条件判断 |

---

### Int32 (0x0040-0x005F)

| 指令名      | 十六进制   | 说明             |
|----------|--------|----------------|
| I\_ADD   | 0x0040 | int32 加法       |
| I\_SUB   | 0x0041 | int32 减法       |
| I\_MUL   | 0x0042 | int32 乘法       |
| I\_DIV   | 0x0043 | int32 除法       |
| I\_MOD   | 0x0044 | int32 取余       |
| I\_NEG   | 0x0045 | int32 取负       |
| I\_INC   | 0x0046 | int32 自增       |
| I\_AND   | 0x0047 | int32 按位与      |
| I\_OR    | 0x0048 | int32 按位或      |
| I\_XOR   | 0x0049 | int32 按位异或     |
| I\_PUSH  | 0x004A | int32 入栈       |
| I\_LOAD  | 0x004B | int32 本地变量加载   |
| I\_STORE | 0x004C | int32 本地变量存储   |
| I\_CE    | 0x004D | int32 等于条件判断   |
| I\_CNE   | 0x004E | int32 不等于条件判断  |
| I\_CG    | 0x004F | int32 大于条件判断   |
| I\_CGE   | 0x0050 | int32 大于等于条件判断 |
| I\_CL    | 0x0051 | int32 小于条件判断   |
| I\_CLE   | 0x0052 | int32 小于等于条件判断 |

---

### Long64 区域（0x0060-0x007F）

| 指令名      | 十六进制   | 说明              |
|----------|--------|-----------------|
| L\_ADD   | 0x0060 | long64 加法       |
| L\_SUB   | 0x0061 | long64 减法       |
| L\_MUL   | 0x0062 | long64 乘法       |
| L\_DIV   | 0x0063 | long64 除法       |
| L\_MOD   | 0x0064 | long64 取余       |
| L\_NEG   | 0x0065 | long64 取负       |
| L\_INC   | 0x0066 | long64 自增       |
| L\_AND   | 0x0067 | long64 按位与      |
| L\_OR    | 0x0068 | long64 按位或      |
| L\_XOR   | 0x0069 | long64 按位异或     |
| L\_PUSH  | 0x006A | long64 入栈       |
| L\_LOAD  | 0x006B | long64 本地变量加载   |
| L\_STORE | 0x006C | long64 本地变量存储   |
| L\_CE    | 0x006D | long64 等于条件判断   |
| L\_CNE   | 0x006E | long64 不等于条件判断  |
| L\_CG    | 0x006F | long64 大于条件判断   |
| L\_CGE   | 0x0070 | long64 大于等于条件判断 |
| L\_CL    | 0x0071 | long64 小于条件判断   |
| L\_CLE   | 0x0072 | long64 小于等于条件判断 |

---

### Float32 区域（0x0080-0x009F）

| 指令名      | 十六进制   | 说明               |
|----------|--------|------------------|
| F\_ADD   | 0x0080 | float32 加法       |
| F\_SUB   | 0x0081 | float32 减法       |
| F\_MUL   | 0x0082 | float32 乘法       |
| F\_DIV   | 0x0083 | float32 除法       |
| F\_MOD   | 0x0084 | float32 取余       |
| F\_NEG   | 0x0085 | float32 取负       |
| F\_INC   | 0x0086 | float32 自增       |
| F\_PUSH  | 0x0087 | float32 入栈       |
| F\_LOAD  | 0x0088 | float32 本地变量加载   |
| F\_STORE | 0x0089 | float32 本地变量存储   |
| F\_CE    | 0x008A | float32 等于条件判断   |
| F\_CNE   | 0x008B | float32 不等于条件判断  |
| F\_CG    | 0x008C | float32 大于条件判断   |
| F\_CGE   | 0x008D | float32 大于等于条件判断 |
| F\_CL    | 0x008E | float32 小于条件判断   |
| F\_CLE   | 0x008F | float32 小于等于条件判断 |

---

### Double64 区域（0x00A0-0x00BF）

| 指令名      | 十六进制   | 说明                |
|----------|--------|-------------------|
| D\_ADD   | 0x00A0 | double64 加法       |
| D\_SUB   | 0x00A1 | double64 减法       |
| D\_MUL   | 0x00A2 | double64 乘法       |
| D\_DIV   | 0x00A3 | double64 除法       |
| D\_MOD   | 0x00A4 | double64 取余       |
| D\_NEG   | 0x00A5 | double64 取负       |
| D\_INC   | 0x00A6 | double64 自增       |
| D\_PUSH  | 0x00A7 | double64 入栈       |
| D\_LOAD  | 0x00A8 | double64 本地变量加载   |
| D\_STORE | 0x00A9 | double64 本地变量存储   |
| D\_CE    | 0x00AA | double64 等于条件判断   |
| D\_CNE   | 0x00AB | double64 不等于条件判断  |
| D\_CG    | 0x00AC | double64 大于条件判断   |
| D\_CGE   | 0x00AD | double64 大于等于条件判断 |
| D\_CL    | 0x00AE | double64 小于条件判断   |
| D\_CLE   | 0x00AF | double64 小于等于条件判断 |

---

## Type Conversion (0x00C0-0x00DF)

| 指令名 | 十六进制   | 说明                 |
|-----|--------|--------------------|
| B2S | 0x00C0 | byte8 转 short16    |
| B2I | 0x00C1 | byte8 转 int32      |
| B2L | 0x00C2 | byte8 转 long64     |
| B2F | 0x00C3 | byte8 转 float32    |
| B2D | 0x00C4 | byte8 转 double64   |
| S2B | 0x00C5 | short16 转 byte8    |
| S2I | 0x00C6 | short16 转 int32    |
| S2L | 0x00C7 | short16 转 long64   |
| S2F | 0x00C8 | short16 转 float32  |
| S2D | 0x00C9 | short16 转 double64 |
| I2B | 0x00CA | int32 转 byte8      |
| I2S | 0x00CB | int32 转 short16    |
| I2L | 0x00CC | int32 转 long64     |
| I2F | 0x00CD | int32 转 float32    |
| I2D | 0x00CE | int32 转 double64   |
| L2B | 0x00CF | long64 转 byte8     |
| L2S | 0x00D0 | long64 转 short16   |
| L2I | 0x00D1 | long64 转 int32     |
| L2F | 0x00D2 | long64 转 float32   |
| L2D | 0x00D3 | long64 转 double64  |
| F2B | 0x00D4 | float32 转 byte8    |
| F2S | 0x00D5 | float32 转 short16  |
| F2I | 0x00D6 | float32 转 int32    |
| F2L | 0x00D7 | float32 转 long64   |
| F2D | 0x00D8 | float32 转 double64 |
| D2B | 0x00D9 | double64 转 byte8   |
| D2S | 0x00DA | double64 转 short16 |
| D2I | 0x00DB | double64 转 int32   |
| D2L | 0x00DC | double64 转 long64  |
| D2F | 0x00DD | double64 转 float32 |

---
## Reference Control (0x00E0-0x00EF)
| 指令名      | 十六进制   | 说明                        |
|----------|--------|---------------------------|
| R\_PUSH  | 0x00E0 | 将对象引用压入操作数栈               |
| R\_LOAD  | 0x00E1 | 从本地变量表加载对象引用并压入操作数栈       |
| R\_STORE | 0x00E2 | 将操作数栈顶的对象引用弹出并存入本地变量表指定槽位 |



## Stack Control (0x0100-0x01FF)

| 指令名  | 十六进制   | 说明       |
|------|--------|----------|
| POP  | 0x0100 | 弹出栈顶元素   |
| DUP  | 0x0101 | 复制栈顶元素   |
| SWAP | 0x0102 | 交换栈顶前两元素 |

---

## Flow Control (0x0200-0x02FF)

| 指令名  | 十六进制/十进制 | 说明    |
|------|----------|-------|
| JUMP | 0x0200   | 无条件跳转 |
| CALL | 0x0201   | 子程序调用 |
| RET  | 0x0202   | 子程序返回 |

---

##  Register Control (0x0300-0x03FF)

| 指令名 | 十六进制   | 说明      |
|-----|--------|---------|
| MOV | 0x0300 | 局部变量间赋值 |

---

## System Control (0x0400-0x04FF)

| 指令名         | 十六进制   | 说明   |
|-------------|--------|------|
| HALT        | 0x0400 | 程序终止 |
| SYSCALL     | 0x0401 | 系统调用 |
| DEBUG\_TRAP | 0x0402 | 调试断点 |