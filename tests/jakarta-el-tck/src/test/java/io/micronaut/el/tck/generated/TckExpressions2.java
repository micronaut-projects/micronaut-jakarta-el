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
@io.micronaut.el.annotation.ELExpression(value = "#{24 / 2 == 10 ? false : true}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_0")
@io.micronaut.el.annotation.ELExpression(value = "#{24 div 2 == 10 ? false : true}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_1")
@io.micronaut.el.annotation.ELExpression(value = "#{2}", expectedType = java.lang.String.class, name = "EXPRESSION_3_2")
@io.micronaut.el.annotation.ELExpression(value = "#{3 % 2 == 1}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_3")
@io.micronaut.el.annotation.ELExpression(value = "#{3 % 8 gt 1}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_4")
@io.micronaut.el.annotation.ELExpression(value = "#{3 * 2 < 8}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_5")
@io.micronaut.el.annotation.ELExpression(value = "#{3 > 4  / 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_6")
@io.micronaut.el.annotation.ELExpression(value = "#{3 > 4  div 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_7")
@io.micronaut.el.annotation.ELExpression(value = "#{3 mod 8 gt 1}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_8")
@io.micronaut.el.annotation.ELExpression(value = "#{3 mod 2 == 1}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_9")
@io.micronaut.el.annotation.ELExpression(value = "#{3}", expectedType = java.lang.String.class, name = "EXPRESSION_3_10")
@io.micronaut.el.annotation.ELExpression(value = "#{true} true", expectedType = java.lang.Boolean.class, name = "EXPRESSION_3_11")
@io.micronaut.el.annotation.ELExpression(value = "#{worker.firstName}", expectedType = java.lang.String.class, name = "EXPRESSION_3_12")
@io.micronaut.el.annotation.ELExpression(value = "#{worker.lastName}", expectedType = java.lang.String.class, name = "EXPRESSION_3_13")
@io.micronaut.el.annotation.ELExpression(value = "#{A - B}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_14")
@io.micronaut.el.annotation.ELExpression(value = "#{A / B}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_15")
@io.micronaut.el.annotation.ELExpression(value = "#{A % B}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_16")
@io.micronaut.el.annotation.ELExpression(value = "#{A && B}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_17")
@io.micronaut.el.annotation.ELExpression(value = "#{A * B}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_18")
@io.micronaut.el.annotation.ELExpression(value = "#{A += B}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_19")
@io.micronaut.el.annotation.ELExpression(value = "#{A + B}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_20")
@io.micronaut.el.annotation.ELExpression(value = "#{A ?B: C}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_21")
@io.micronaut.el.annotation.ELExpression(value = "#{A or B}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_22")
@io.micronaut.el.annotation.ELExpression(value = "#{A and B}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_23")
@io.micronaut.el.annotation.ELExpression(value = "#{A ge B}", expectedType = java.lang.Boolean.class, name = "EXPRESSION_3_24")
@io.micronaut.el.annotation.ELExpression(value = "#{A le B}", expectedType = java.lang.Boolean.class, name = "EXPRESSION_3_25")
@io.micronaut.el.annotation.ELExpression(value = "#{A || B}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_26")
@io.micronaut.el.annotation.ELExpression(value = "#{A}", expectedType = java.lang.Boolean.class, name = "EXPRESSION_3_27")
@io.micronaut.el.annotation.ELExpression(value = "#{A}", expectedType = java.lang.Byte.class, name = "EXPRESSION_3_28")
@io.micronaut.el.annotation.ELExpression(value = "#{A}", expectedType = java.lang.Float.class, name = "EXPRESSION_3_29")
@io.micronaut.el.annotation.ELExpression(value = "#{A}", expectedType = java.lang.Integer.class, name = "EXPRESSION_3_30")
@io.micronaut.el.annotation.ELExpression(value = "#{A}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_31")
@io.micronaut.el.annotation.ELExpression(value = "#{SERIAL} SERIAL", expectedType = java.lang.String.class, name = "EXPRESSION_3_32")
@io.micronaut.el.annotation.ELExpression(value = "#{empty A}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_33")
@io.micronaut.el.annotation.ELExpression(value = "#{empty null}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_34")
@io.micronaut.el.annotation.ELExpression(value = "#{employee.firstname}#{employee.lastname}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_35")
@io.micronaut.el.annotation.ELExpression(value = "#{employee.lastname}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_36")
@io.micronaut.el.annotation.ELExpression(value = "#{false}", expectedType = java.lang.String.class, name = "EXPRESSION_3_37")
@io.micronaut.el.annotation.ELExpression(value = "#{not A}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_38")
@io.micronaut.el.annotation.ELExpression(value = "#{null}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_39")
@io.micronaut.el.annotation.ELExpression(value = "#{null}", expectedType = java.util.Date.class, name = "EXPRESSION_3_40")
@io.micronaut.el.annotation.ELExpression(value = "${4 % 15 > 1}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_41")
@io.micronaut.el.annotation.ELExpression(value = "${4 >= 6 / 24}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_42")
@io.micronaut.el.annotation.ELExpression(value = "${4 >= 6 div 24}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_43")
@io.micronaut.el.annotation.ELExpression(value = "${4 mod 15 > 1}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_44")
@io.micronaut.el.annotation.ELExpression(value = "${4}", expectedType = java.lang.Character.class, name = "EXPRESSION_3_45")
@io.micronaut.el.annotation.ELExpression(value = "${5 != 5 % 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_46")
@io.micronaut.el.annotation.ELExpression(value = "${5 != 5 mod 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_47")
@io.micronaut.el.annotation.ELExpression(value = "${5 * 1 ge 6}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_48")
@io.micronaut.el.annotation.ELExpression(value = "${5 * 1 gt 6}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_49")
@io.micronaut.el.annotation.ELExpression(value = "${5 * 2 == 10 ? false : true}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_50")
@io.micronaut.el.annotation.ELExpression(value = "${5 * 2 != 10}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_51")
@io.micronaut.el.annotation.ELExpression(value = "${5 * 5 == 10 ? false : true}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_52")
@io.micronaut.el.annotation.ELExpression(value = "${5 == 5 * 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_53")
@io.micronaut.el.annotation.ELExpression(value = "${5 < 6 % 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_54")
@io.micronaut.el.annotation.ELExpression(value = "${5 < 6 mod 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_55")
@io.micronaut.el.annotation.ELExpression(value = "${5 eq 5 / 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_56")
@io.micronaut.el.annotation.ELExpression(value = "${5 eq 5 div 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_57")
@io.micronaut.el.annotation.ELExpression(value = "${5.0}", expectedType = java.lang.Character.class, name = "EXPRESSION_3_58")
@io.micronaut.el.annotation.ELExpression(value = "${50 / 2 le 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_59")
@io.micronaut.el.annotation.ELExpression(value = "${50 div 2 le 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_60")
@io.micronaut.el.annotation.ELExpression(value = "${5000000.0}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_61")
@io.micronaut.el.annotation.ELExpression(value = "${6 % 2 eq 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_62")
@io.micronaut.el.annotation.ELExpression(value = "${6 % 29 >= 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_63")
@io.micronaut.el.annotation.ELExpression(value = "${6 * 2 <= 12}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_64")
@io.micronaut.el.annotation.ELExpression(value = "${6 >= 5 * 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_65")
@io.micronaut.el.annotation.ELExpression(value = "${6 > 5 * 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_66")
@io.micronaut.el.annotation.ELExpression(value = "${6 <= 5 % 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_67")
@io.micronaut.el.annotation.ELExpression(value = "${6 <= 5 mod 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_68")
@io.micronaut.el.annotation.ELExpression(value = "${6 mod 2 eq 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_69")
@io.micronaut.el.annotation.ELExpression(value = "${6 mod 29 >= 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_70")
@io.micronaut.el.annotation.ELExpression(value = "${6 gt 5 / 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_71")
@io.micronaut.el.annotation.ELExpression(value = "${6 gt 5 div 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_72")
@io.micronaut.el.annotation.ELExpression(value = "${6 ge 5 / 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_73")
@io.micronaut.el.annotation.ELExpression(value = "${6 ge 5 div 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_74")
@io.micronaut.el.annotation.ELExpression(value = "${6 lt 5 % 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_75")
@io.micronaut.el.annotation.ELExpression(value = "${6 lt 5 * 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_76")
@io.micronaut.el.annotation.ELExpression(value = "${6 lt 5 mod 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_77")
@io.micronaut.el.annotation.ELExpression(value = "${6 le 5 % 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_78")
@io.micronaut.el.annotation.ELExpression(value = "${6 le 5 * 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_79")
@io.micronaut.el.annotation.ELExpression(value = "${6 le 5 mod 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_80")
@io.micronaut.el.annotation.ELExpression(value = "${6.5}", expectedType = java.lang.Character.class, name = "EXPRESSION_3_81")
@io.micronaut.el.annotation.ELExpression(value = "${7}", expectedType = java.lang.Character.class, name = "EXPRESSION_3_82")
@io.micronaut.el.annotation.ELExpression(value = "${8 % 5 ge 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_83")
@io.micronaut.el.annotation.ELExpression(value = "${8 mod 5 ge 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_84")
@io.micronaut.el.annotation.ELExpression(value = "${8.1}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_85")
@io.micronaut.el.annotation.ELExpression(value = "${8.1E-9}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_86")
@io.micronaut.el.annotation.ELExpression(value = "${8100.0}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_87")
@io.micronaut.el.annotation.ELExpression(value = "${81000.0}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_88")
@io.micronaut.el.annotation.ELExpression(value = "${8100000.0}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_89")
@io.micronaut.el.annotation.ELExpression(value = "${ A + B\t+\t\tC\t}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_90")
@io.micronaut.el.annotation.ELExpression(value = "${ ['a', 'b', 'b', 'c'].stream().distinct().toList()}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_91")
@io.micronaut.el.annotation.ELExpression(value = "${! A}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_92")
@io.micronaut.el.annotation.ELExpression(value = "${\"STRING\"}", expectedType = java.lang.String.class, name = "EXPRESSION_3_93")
@io.micronaut.el.annotation.ELExpression(value = "${\"foo\"}", expectedType = java.lang.String.class, name = "EXPRESSION_3_94")
@io.micronaut.el.annotation.ELExpression(value = "${'\"catstring\"'}", expectedType = java.lang.Object.class, name = "EXPRESSION_3_95")
@io.micronaut.el.annotation.ELExpression(value = "${''}", expectedType = java.lang.Byte.class, name = "EXPRESSION_3_96")
@io.micronaut.el.annotation.ELExpression(value = "${''}", expectedType = java.lang.Character.class, name = "EXPRESSION_3_97")
@io.micronaut.el.annotation.ELExpression(value = "${''}", expectedType = java.lang.Double.class, name = "EXPRESSION_3_98")
@io.micronaut.el.annotation.ELExpression(value = "${''}", expectedType = java.lang.Float.class, name = "EXPRESSION_3_99")
final class TckExpressions2 {
}
