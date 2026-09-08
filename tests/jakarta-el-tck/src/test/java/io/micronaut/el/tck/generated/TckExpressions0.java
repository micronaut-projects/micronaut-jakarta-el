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
@io.micronaut.el.annotation.ELMethodExpression(value = "#{vect.noSuchMethod}", expectedReturnType = java.lang.Boolean.class, expectedParamTypes = {java.lang.Object.class}, name = "EXPRESSION_1_0")
@io.micronaut.el.annotation.ELMethodExpression(value = "#{vect.add}", expectedReturnType = java.lang.Object.class, expectedParamTypes = {java.lang.Integer.class, java.lang.Object.class}, name = "EXPRESSION_1_1")
@io.micronaut.el.annotation.ELMethodExpression(value = "#{vect.add}", expectedReturnType = java.lang.Boolean.class, expectedParamTypes = {java.lang.Object.class}, name = "EXPRESSION_1_2")
@io.micronaut.el.annotation.ELMethodExpression(value = "#{vect.add}", expectedReturnType = java.lang.Boolean.class, expectedParamTypes = {java.lang.Object.class}, name = "EXPRESSION_1_3")
@io.micronaut.el.annotation.ELMethodExpression(value = "#{vect.add}", expectedReturnType = java.lang.Object.class, name = "EXPRESSION_1_4")
@io.micronaut.el.annotation.ELMethodExpression(value = "#{vect[add]}", expectedReturnType = java.lang.Object.class, name = "EXPRESSION_1_5")
@io.micronaut.el.annotation.ELMethodExpression(value = "#{wect.add}", expectedReturnType = java.lang.Boolean.class, expectedParamTypes = {java.lang.Object.class}, name = "EXPRESSION_1_6")
@io.micronaut.el.annotation.ELMethodExpression(value = "#{add}", expectedReturnType = java.lang.Object.class, name = "EXPRESSION_1_7")
@io.micronaut.el.annotation.ELMethodExpression(value = "#{bean.noSuchMethod()}", expectedReturnType = java.lang.String.class, name = "EXPRESSION_1_8")
@io.micronaut.el.annotation.ELMethodExpression(value = "#{bean.targetA('text')}", expectedReturnType = java.lang.String.class, name = "EXPRESSION_1_9")
@io.micronaut.el.annotation.ELMethodExpression(value = "#{bean.targetB('1')}", expectedReturnType = java.lang.String.class, name = "EXPRESSION_1_10")
@io.micronaut.el.annotation.ELMethodExpression(value = "#{bean.targetC('aaa','bbb')}", expectedReturnType = java.lang.String.class, name = "EXPRESSION_1_11")
@io.micronaut.el.annotation.ELMethodExpression(value = "#{bean.targetD('aaa','bbb')}", expectedReturnType = java.lang.String.class, name = "EXPRESSION_1_12")
@io.micronaut.el.annotation.ELMethodExpression(value = "#{bean.targetD('1','1')}", expectedReturnType = java.lang.String.class, name = "EXPRESSION_1_13")
@io.micronaut.el.annotation.ELMethodExpression(value = "#{bean.targetE('1234',1234)}", expectedReturnType = java.lang.String.class, name = "EXPRESSION_1_14")
@io.micronaut.el.annotation.ELMethodExpression(value = "#{bean.targetF('aaa',1234)}", expectedReturnType = java.lang.String.class, name = "EXPRESSION_1_15")
@io.micronaut.el.annotation.ELMethodExpression(value = "#{noSuchBean.method()}", expectedReturnType = java.lang.String.class, name = "EXPRESSION_1_16")
@io.micronaut.el.annotation.ELMethodExpression(value = "${vect.add}", expectedReturnType = java.lang.Object.class, name = "EXPRESSION_1_17")
@io.micronaut.el.annotation.ELMethodExpression(value = "${vect[add]}", expectedReturnType = java.lang.Object.class, name = "EXPRESSION_1_18")
@io.micronaut.el.annotation.ELMethodExpression(value = "${add}", expectedReturnType = java.lang.Object.class, name = "EXPRESSION_1_19")
@io.micronaut.el.annotation.ELMethodExpression(value = "${foo}", expectedReturnType = java.lang.Object.class, name = "EXPRESSION_1_20")
@io.micronaut.el.annotation.ELExpression(value = "#{4 % 15 > 1}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_21")
@io.micronaut.el.annotation.ELExpression(value = "#{4 >= 6 / 24}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_22")
@io.micronaut.el.annotation.ELExpression(value = "#{4 >= 6 div 24}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_23")
@io.micronaut.el.annotation.ELExpression(value = "#{4 mod 15 > 1}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_24")
@io.micronaut.el.annotation.ELExpression(value = "#{4}", expectedType = java.lang.String.class, name = "EXPRESSION_1_25")
@io.micronaut.el.annotation.ELExpression(value = "#{5 != 5 % 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_26")
@io.micronaut.el.annotation.ELExpression(value = "#{5 != 5 mod 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_27")
@io.micronaut.el.annotation.ELExpression(value = "#{5 * 1 ge 6}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_28")
@io.micronaut.el.annotation.ELExpression(value = "#{5 * 1 gt 6}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_29")
@io.micronaut.el.annotation.ELExpression(value = "#{5 * 2 == 10 ? false : true}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_30")
@io.micronaut.el.annotation.ELExpression(value = "#{5 * 2 != 10}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_31")
@io.micronaut.el.annotation.ELExpression(value = "#{5 * 5 == 10 ? false : true}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_32")
@io.micronaut.el.annotation.ELExpression(value = "#{5 == 5 * 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_33")
@io.micronaut.el.annotation.ELExpression(value = "#{5 < 6 % 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_34")
@io.micronaut.el.annotation.ELExpression(value = "#{5 < 6 mod 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_35")
@io.micronaut.el.annotation.ELExpression(value = "#{5 eq 5 / 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_36")
@io.micronaut.el.annotation.ELExpression(value = "#{5 eq 5 div 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_37")
@io.micronaut.el.annotation.ELExpression(value = "#{5.5}", expectedType = java.lang.String.class, name = "EXPRESSION_1_38")
@io.micronaut.el.annotation.ELExpression(value = "#{50 / 2 le 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_39")
@io.micronaut.el.annotation.ELExpression(value = "#{50 div 2 le 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_40")
@io.micronaut.el.annotation.ELExpression(value = "#{5000000.0}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_41")
@io.micronaut.el.annotation.ELExpression(value = "#{6 % 2 eq 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_42")
@io.micronaut.el.annotation.ELExpression(value = "#{6 % 29 >= 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_43")
@io.micronaut.el.annotation.ELExpression(value = "#{6 * 2 <= 12}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_44")
@io.micronaut.el.annotation.ELExpression(value = "#{6 >= 5 * 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_45")
@io.micronaut.el.annotation.ELExpression(value = "#{6 > 5 * 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_46")
@io.micronaut.el.annotation.ELExpression(value = "#{6 <= 5 % 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_47")
@io.micronaut.el.annotation.ELExpression(value = "#{6 <= 5 mod 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_48")
@io.micronaut.el.annotation.ELExpression(value = "#{6 mod 2 eq 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_49")
@io.micronaut.el.annotation.ELExpression(value = "#{6 mod 29 >= 5}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_50")
@io.micronaut.el.annotation.ELExpression(value = "#{6 gt 5 / 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_51")
@io.micronaut.el.annotation.ELExpression(value = "#{6 gt 5 div 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_52")
@io.micronaut.el.annotation.ELExpression(value = "#{6 ge 5 / 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_53")
@io.micronaut.el.annotation.ELExpression(value = "#{6 ge 5 div 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_54")
@io.micronaut.el.annotation.ELExpression(value = "#{6 lt 5 % 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_55")
@io.micronaut.el.annotation.ELExpression(value = "#{6 lt 5 * 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_56")
@io.micronaut.el.annotation.ELExpression(value = "#{6 lt 5 mod 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_57")
@io.micronaut.el.annotation.ELExpression(value = "#{6 le 5 % 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_58")
@io.micronaut.el.annotation.ELExpression(value = "#{6 le 5 * 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_59")
@io.micronaut.el.annotation.ELExpression(value = "#{6 le 5 mod 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_60")
@io.micronaut.el.annotation.ELExpression(value = "#{6.5}", expectedType = java.lang.String.class, name = "EXPRESSION_1_61")
@io.micronaut.el.annotation.ELExpression(value = "#{8 % 5 ge 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_62")
@io.micronaut.el.annotation.ELExpression(value = "#{8 mod 5 ge 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_63")
@io.micronaut.el.annotation.ELExpression(value = "#{8.1}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_64")
@io.micronaut.el.annotation.ELExpression(value = "#{8.1E-9}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_65")
@io.micronaut.el.annotation.ELExpression(value = "#{8100.0}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_66")
@io.micronaut.el.annotation.ELExpression(value = "#{81000.0}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_67")
@io.micronaut.el.annotation.ELExpression(value = "#{8100000.0}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_68")
@io.micronaut.el.annotation.ELExpression(value = "#{! A}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_69")
@io.micronaut.el.annotation.ELExpression(value = "#{\"foo\"}", expectedType = java.lang.String.class, name = "EXPRESSION_1_70")
@io.micronaut.el.annotation.ELExpression(value = "#{'#{'}foo}", expectedType = java.lang.String.class, name = "EXPRESSION_1_71")
@io.micronaut.el.annotation.ELExpression(value = "#{'\"catstring\"'}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_72")
@io.micronaut.el.annotation.ELExpression(value = "#{''}", expectedType = java.lang.Boolean.class, name = "EXPRESSION_1_73")
@io.micronaut.el.annotation.ELExpression(value = "#{'\\'pullstring\\''}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_74")
@io.micronaut.el.annotation.ELExpression(value = "#{'hello'}", expectedType = java.lang.String.class, name = "EXPRESSION_1_75")
@io.micronaut.el.annotation.ELExpression(value = "#{'str\\\\ing'}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_76")
@io.micronaut.el.annotation.ELExpression(value = "#{'string'}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_77")
@io.micronaut.el.annotation.ELExpression(value = "#{'true'}", expectedType = java.lang.Boolean.class, name = "EXPRESSION_1_78")
@io.micronaut.el.annotation.ELExpression(value = "#{'x'}", expectedType = java.lang.Character.class, name = "EXPRESSION_1_79")
@io.micronaut.el.annotation.ELExpression(value = "#{(1 + 5) * 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_80")
@io.micronaut.el.annotation.ELExpression(value = "#{(1 - 5) + 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_81")
@io.micronaut.el.annotation.ELExpression(value = "#{(2 + 3) - 10}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_82")
@io.micronaut.el.annotation.ELExpression(value = "#{(2 + 7) % 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_83")
@io.micronaut.el.annotation.ELExpression(value = "#{(4 + 4) / 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_84")
@io.micronaut.el.annotation.ELExpression(value = "#{-0.72}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_85")
@io.micronaut.el.annotation.ELExpression(value = "#{-0.003444}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_86")
@io.micronaut.el.annotation.ELExpression(value = "#{-1.0}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_87")
@io.micronaut.el.annotation.ELExpression(value = "#{-10.0}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_88")
@io.micronaut.el.annotation.ELExpression(value = "#{-2147483647}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_89")
@io.micronaut.el.annotation.ELExpression(value = "#{-2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_90")
@io.micronaut.el.annotation.ELExpression(value = "#{-344400.0}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_91")
@io.micronaut.el.annotation.ELExpression(value = "#{-34.44}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_92")
@io.micronaut.el.annotation.ELExpression(value = "#{-70.2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_93")
@io.micronaut.el.annotation.ELExpression(value = "#{-A}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_94")
@io.micronaut.el.annotation.ELExpression(value = "#{-null}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_95")
@io.micronaut.el.annotation.ELExpression(value = "#{0.999}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_96")
@io.micronaut.el.annotation.ELExpression(value = "#{0}", expectedType = java.lang.String.class, name = "EXPRESSION_1_97")
@io.micronaut.el.annotation.ELExpression(value = "#{1 - 4 / 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_98")
@io.micronaut.el.annotation.ELExpression(value = "#{1 - 4 div 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_1_99")
final class TckExpressions0 {
}
