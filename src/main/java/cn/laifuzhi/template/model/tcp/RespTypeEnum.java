package cn.laifuzhi.template.model.tcp;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum RespTypeEnum {
    OK((byte) 0, "success"),
    PARAM_ERROR((byte) 1, "parameter error"),
    UNKNOWN_ERROR((byte) 2, "unknown error"),
    ;
    private static final Map<Byte, RespTypeEnum> typeEnumMap = new HashMap<>();

    private final byte type;
    private final String decs;

    static {
        for (RespTypeEnum respTypeEnum : values()) {
            typeEnumMap.put(respTypeEnum.getType(), respTypeEnum);
        }
    }

    public static Optional<RespTypeEnum> getByType(byte type) {
        return Optional.ofNullable(typeEnumMap.get(type));
    }

    public static boolean contains(byte type) {
        return typeEnumMap.containsKey(type);
    }
}
