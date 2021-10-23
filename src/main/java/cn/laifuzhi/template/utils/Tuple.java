package cn.laifuzhi.template.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public final class Tuple<T1, T2> {
    private T1 t1;
    private T2 t2;
}
