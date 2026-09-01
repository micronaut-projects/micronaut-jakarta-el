/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.el.tck.generated;

@io.micronaut.el.annotation.ELEnvironment(variables = {
    @io.micronaut.el.annotation.ELVariable(name = "A", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "B", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "C", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "Doe", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "Int", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "John", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "Name", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "SERIAL", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "a", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "aaa", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "add", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "b", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "bar", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "bbb", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "bean", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "c", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "comparing", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "cond", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "customers", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "employee", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "f", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "foo", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "func", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "i", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "ints", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "j", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "javabook", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "l", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "literal", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "lst", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "map", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "noSuchBean", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "nullValue", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "p", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "products", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "q", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "r", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "tem", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "total", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "types", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "val", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "vect", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "wect", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "worker", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "x", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "y", type = java.lang.Object.class),
    @io.micronaut.el.annotation.ELVariable(name = "z", type = java.lang.Object.class)
}, functions = {
    @io.micronaut.el.annotation.ELFunctions(value = io.micronaut.el.tck.TckFunctions.class),
    @io.micronaut.el.annotation.ELFunctions(prefix = "Int", value = io.micronaut.el.tck.TckFunctions.class)
})
@io.micronaut.el.annotation.ELExpression(value = "${1 + 7 % 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_0")
@io.micronaut.el.annotation.ELExpression(value = "${1 + 7 mod 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_1")
@io.micronaut.el.annotation.ELExpression(value = "${1 == 2 / 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_2")
@io.micronaut.el.annotation.ELExpression(value = "${1 == 2 div 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_3")
@io.micronaut.el.annotation.ELExpression(value = "${1 == nullValue}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_4")
@io.micronaut.el.annotation.ELExpression(value = "${1 >= nullValue}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_5")
@io.micronaut.el.annotation.ELExpression(value = "${1 <= nullValue}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_6")
@io.micronaut.el.annotation.ELExpression(value = "${10 == 5 * 2 && 6 * 2 == 12}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_7")
@io.micronaut.el.annotation.ELExpression(value = "${10 == 5 * 2 && 6 * 2 == 15}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_8")
@io.micronaut.el.annotation.ELExpression(value = "${10 == 5 * 2 || 6 * 2 == 15}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_9")
@io.micronaut.el.annotation.ELExpression(value = "${10 == 5 * 2 and 6 * 2 == 12}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_10")
@io.micronaut.el.annotation.ELExpression(value = "${10 == 5 * 2 and 6 * 2 == 15}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_11")
@io.micronaut.el.annotation.ELExpression(value = "${10 == 5 * 2 or 6 * 2 == 15}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_12")
@io.micronaut.el.annotation.ELExpression(value = "${10 == 5 * 5 || 6 * 2 == 12}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_13")
@io.micronaut.el.annotation.ELExpression(value = "${10 == 5 * 5 || 6 * 6 == 12}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_14")
@io.micronaut.el.annotation.ELExpression(value = "${10 == 5 * 5 or 6 * 2 == 12}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_15")
@io.micronaut.el.annotation.ELExpression(value = "${10 == 5 * 5 or 6 * 6 == 12}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_16")
@io.micronaut.el.annotation.ELExpression(value = "${10 - (2 + 3)}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_17")
@io.micronaut.el.annotation.ELExpression(value = "${10 / 5 != 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_18")
@io.micronaut.el.annotation.ELExpression(value = "${10 div 5 != 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_19")
@io.micronaut.el.annotation.ELExpression(value = "${10 eq 5 * 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_20")
@io.micronaut.el.annotation.ELExpression(value = "${100.5}", expectedType = java.lang.Byte.class, name = "EXPRESSION_5_21")
@io.micronaut.el.annotation.ELExpression(value = "${100.5}", expectedType = java.lang.Double.class, name = "EXPRESSION_5_22")
@io.micronaut.el.annotation.ELExpression(value = "${100.5}", expectedType = java.lang.Float.class, name = "EXPRESSION_5_23")
@io.micronaut.el.annotation.ELExpression(value = "${100.5}", expectedType = java.lang.Integer.class, name = "EXPRESSION_5_24")
@io.micronaut.el.annotation.ELExpression(value = "${100.5}", expectedType = java.lang.Long.class, name = "EXPRESSION_5_25")
@io.micronaut.el.annotation.ELExpression(value = "${100.5}", expectedType = java.lang.Short.class, name = "EXPRESSION_5_26")
@io.micronaut.el.annotation.ELExpression(value = "${100.5}", expectedType = java.math.BigDecimal.class, name = "EXPRESSION_5_27")
@io.micronaut.el.annotation.ELExpression(value = "${100.5}", expectedType = java.math.BigInteger.class, name = "EXPRESSION_5_28")
@io.micronaut.el.annotation.ELExpression(value = "${12 / 1 lt 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_29")
@io.micronaut.el.annotation.ELExpression(value = "${12 / 2 < 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_30")
@io.micronaut.el.annotation.ELExpression(value = "${12 / 2 == 6 && 10 / 2  == 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_31")
@io.micronaut.el.annotation.ELExpression(value = "${12 / 2 == 6 || 10 / 5  == 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_32")
@io.micronaut.el.annotation.ELExpression(value = "${12 / 2 == 6 and 10 / 2  == 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_33")
@io.micronaut.el.annotation.ELExpression(value = "${12 / 2 == 6 or 10 / 5  == 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_34")
@io.micronaut.el.annotation.ELExpression(value = "${12 / 3 == 6 && 10 / 2  == 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_35")
@io.micronaut.el.annotation.ELExpression(value = "${12 / 3 == 6 || 10 / 2  == 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_36")
@io.micronaut.el.annotation.ELExpression(value = "${12 / 3 == 6 || 10 / 5  == 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_37")
@io.micronaut.el.annotation.ELExpression(value = "${12 / 3 == 6 and 10 / 2  == 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_38")
@io.micronaut.el.annotation.ELExpression(value = "${12 / 3 == 6 or 10 / 2  == 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_39")
@io.micronaut.el.annotation.ELExpression(value = "${12 / 3 == 6 or 10 / 5  == 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_40")
@io.micronaut.el.annotation.ELExpression(value = "${12 div 1 lt 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_41")
@io.micronaut.el.annotation.ELExpression(value = "${12 div 2 < 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_42")
@io.micronaut.el.annotation.ELExpression(value = "${12 div 2 == 6 && 10 div 2  == 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_43")
@io.micronaut.el.annotation.ELExpression(value = "${12 div 2 == 6 or 10 div 5  == 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_44")
@io.micronaut.el.annotation.ELExpression(value = "${12 div 2 == 6 and 10 div 2  == 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_45")
@io.micronaut.el.annotation.ELExpression(value = "${12 div 2 == 6 || 10 div 5  == 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_46")
@io.micronaut.el.annotation.ELExpression(value = "${12 div 3 == 6 && 10 div 2  == 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_47")
@io.micronaut.el.annotation.ELExpression(value = "${12 div 3 == 6 or 10 div 5  == 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_48")
@io.micronaut.el.annotation.ELExpression(value = "${12 div 3 == 6 or 10 div 2  == 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_49")
@io.micronaut.el.annotation.ELExpression(value = "${12 div 3 == 6 and 10 div 2  == 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_50")
@io.micronaut.el.annotation.ELExpression(value = "${12 div 3 == 6 || 10 div 5  == 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_51")
@io.micronaut.el.annotation.ELExpression(value = "${12 div 3 == 6 || 10 div 2  == 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_52")
@io.micronaut.el.annotation.ELExpression(value = "${123}", expectedType = java.lang.Integer.class, name = "EXPRESSION_5_53")
@io.micronaut.el.annotation.ELExpression(value = "${125}", expectedType = java.lang.Byte.class, name = "EXPRESSION_5_54")
@io.micronaut.el.annotation.ELExpression(value = "${125}", expectedType = java.lang.Double.class, name = "EXPRESSION_5_55")
@io.micronaut.el.annotation.ELExpression(value = "${125}", expectedType = java.lang.Float.class, name = "EXPRESSION_5_56")
@io.micronaut.el.annotation.ELExpression(value = "${125}", expectedType = java.lang.Integer.class, name = "EXPRESSION_5_57")
@io.micronaut.el.annotation.ELExpression(value = "${125}", expectedType = java.lang.Long.class, name = "EXPRESSION_5_58")
@io.micronaut.el.annotation.ELExpression(value = "${125}", expectedType = java.lang.Short.class, name = "EXPRESSION_5_59")
@io.micronaut.el.annotation.ELExpression(value = "${125}", expectedType = java.math.BigDecimal.class, name = "EXPRESSION_5_60")
@io.micronaut.el.annotation.ELExpression(value = "${125}", expectedType = java.math.BigInteger.class, name = "EXPRESSION_5_61")
@io.micronaut.el.annotation.ELExpression(value = "${15 % 3 == 3 ? false : true}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_62")
@io.micronaut.el.annotation.ELExpression(value = "${15 % 3 == 3 && 3 % 3 == 0}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_63")
@io.micronaut.el.annotation.ELExpression(value = "${15 % 3 == 3 || 8 % 3 == 0}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_64")
@io.micronaut.el.annotation.ELExpression(value = "${15 % 3 == 3 || 3 % 3 == 0}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_65")
@io.micronaut.el.annotation.ELExpression(value = "${15 % 3 == 3 and 3 % 3 == 0}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_66")
@io.micronaut.el.annotation.ELExpression(value = "${15 % 3 == 3 or 8 % 3 == 0}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_67")
@io.micronaut.el.annotation.ELExpression(value = "${15 % 3 == 3 or 3 % 3 == 0}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_68")
@io.micronaut.el.annotation.ELExpression(value = "${15 % 4 == 3 && 3 % 3 == 0}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_69")
@io.micronaut.el.annotation.ELExpression(value = "${15 % 4 == 3 || 4 % 3 == 0}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_70")
@io.micronaut.el.annotation.ELExpression(value = "${15 % 4 == 3 and 3 % 3 == 0}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_71")
@io.micronaut.el.annotation.ELExpression(value = "${15 % 4 == 3 or 4 % 3 == 0}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_72")
@io.micronaut.el.annotation.ELExpression(value = "${15 * 1 ne 1}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_73")
@io.micronaut.el.annotation.ELExpression(value = "${15 mod 3 == 3 && 3 mod 3 == 0}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_74")
@io.micronaut.el.annotation.ELExpression(value = "${15 mod 3 == 3 ? false : true}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_75")
@io.micronaut.el.annotation.ELExpression(value = "${15 mod 3 == 3 or 3 mod 3 == 0}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_76")
@io.micronaut.el.annotation.ELExpression(value = "${15 mod 3 == 3 or 8 mod 3 == 0}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_77")
@io.micronaut.el.annotation.ELExpression(value = "${15 mod 3 == 3 and 3 mod 3 == 0}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_78")
@io.micronaut.el.annotation.ELExpression(value = "${15 mod 3 == 3 || 3 mod 3 == 0}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_79")
@io.micronaut.el.annotation.ELExpression(value = "${15 mod 3 == 3 || 8 mod 3 == 0}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_80")
@io.micronaut.el.annotation.ELExpression(value = "${15 mod 4 == 3 && 3 mod 3 == 0}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_81")
@io.micronaut.el.annotation.ELExpression(value = "${15 mod 4 == 3 or 4 mod 3 == 0}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_82")
@io.micronaut.el.annotation.ELExpression(value = "${15 mod 4 == 3 and 3 mod 3 == 0}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_83")
@io.micronaut.el.annotation.ELExpression(value = "${15 mod 4 == 3 || 4 mod 3 == 0}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_84")
@io.micronaut.el.annotation.ELExpression(value = "${16 / 2 <= 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_85")
@io.micronaut.el.annotation.ELExpression(value = "${16 div 2 <= 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_86")
@io.micronaut.el.annotation.ELExpression(value = "${18 % (8 + 7)}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_87")
@io.micronaut.el.annotation.ELExpression(value = "${1}", expectedType = java.lang.Boolean.class, name = "EXPRESSION_5_88")
@io.micronaut.el.annotation.ELExpression(value = "${1}", expectedType = java.lang.Character.class, name = "EXPRESSION_5_89")
@io.micronaut.el.annotation.ELExpression(value = "${1}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_90")
@io.micronaut.el.annotation.ELExpression(value = "${2 / (4 + 4)}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_91")
@io.micronaut.el.annotation.ELExpression(value = "${2 * (1 + 5)}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_92")
@io.micronaut.el.annotation.ELExpression(value = "${2 + (5 - 1)}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_93")
@io.micronaut.el.annotation.ELExpression(value = "${2 ne 4 / 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_94")
@io.micronaut.el.annotation.ELExpression(value = "${2 ne 4 div 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_95")
@io.micronaut.el.annotation.ELExpression(value = "${2 ne 5 % 3}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_96")
@io.micronaut.el.annotation.ELExpression(value = "${2 ne 5 mod 3}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_97")
@io.micronaut.el.annotation.ELExpression(value = "${2.34E21}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_98")
@io.micronaut.el.annotation.ELExpression(value = "${20 / 2 == 10 ? false : true}", expectedType = java.lang.Object.class, name = "EXPRESSION_5_99")
final class TckExpressions4 {
}
