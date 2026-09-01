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
@io.micronaut.el.annotation.ELExpression(value = "${A <= B}", expectedType = java.lang.Boolean.class, name = "EXPRESSION_7_0")
@io.micronaut.el.annotation.ELExpression(value = "${A <= B}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_1")
@io.micronaut.el.annotation.ELExpression(value = "${A < B}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_2")
@io.micronaut.el.annotation.ELExpression(value = "${A ne B}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_3")
@io.micronaut.el.annotation.ELExpression(value = "${A eq B}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_4")
@io.micronaut.el.annotation.ELExpression(value = "${A gt B}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_5")
@io.micronaut.el.annotation.ELExpression(value = "${A ge B}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_6")
@io.micronaut.el.annotation.ELExpression(value = "${A lt B}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_7")
@io.micronaut.el.annotation.ELExpression(value = "${A le B}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_8")
@io.micronaut.el.annotation.ELExpression(value = "${A+B+C}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_9")
@io.micronaut.el.annotation.ELExpression(value = "${A}", expectedType = java.lang.Character.class, name = "EXPRESSION_7_10")
@io.micronaut.el.annotation.ELExpression(value = "${A}", expectedType = java.lang.Double.class, name = "EXPRESSION_7_11")
@io.micronaut.el.annotation.ELExpression(value = "${A}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_12")
@io.micronaut.el.annotation.ELExpression(value = "${A}", expectedType = java.util.Date.class, name = "EXPRESSION_7_13")
@io.micronaut.el.annotation.ELExpression(value = "${A}", expectedType = java.lang.Long.class, name = "EXPRESSION_7_14")
@io.micronaut.el.annotation.ELExpression(value = "${A}", expectedType = java.lang.Short.class, name = "EXPRESSION_7_15")
@io.micronaut.el.annotation.ELExpression(value = "${B + A}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_16")
@io.micronaut.el.annotation.ELExpression(value = "${Int:val(\"string\")}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_17")
@io.micronaut.el.annotation.ELExpression(value = "${Int:val(10)}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_18")
@io.micronaut.el.annotation.ELExpression(value = "${SERIAL}", expectedType = java.lang.String.class, name = "EXPRESSION_7_19")
@io.micronaut.el.annotation.ELExpression(value = "${['xy', 'xyz', 'abc'].stream().max().get()}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_20")
@io.micronaut.el.annotation.ELExpression(value = "${['10', '12', '13'].stream().sum()}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_21")
@io.micronaut.el.annotation.ELExpression(value = "${[0,1,2,3,4,5,6].stream().substream(2,5).toList()}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_22")
@io.micronaut.el.annotation.ELExpression(value = "${[0,1,2,3,4].stream().substream(2).toList()}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_23")
@io.micronaut.el.annotation.ELExpression(value = "${[1,2,3,4,5].stream().count()}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_24")
@io.micronaut.el.annotation.ELExpression(value = "${[1,2,3,4,5].stream().reduce((l,r)->l+r).get()}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_25")
@io.micronaut.el.annotation.ELExpression(value = "${[1,2,3,4,5].stream().reduce(0, (l,r)->l+r)}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_26")
@io.micronaut.el.annotation.ELExpression(value = "${[1,2,3,4].stream().filter(i->i > 1).map(i->i*10).toList()}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_27")
@io.micronaut.el.annotation.ELExpression(value = "${[2,3,1,5].stream().max().get()}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_28")
@io.micronaut.el.annotation.ELExpression(value = "${[2,3,1,5].stream().min().get()}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_29")
@io.micronaut.el.annotation.ELExpression(value = "${[2].stream().max((i,j)->i-j).get()}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_30")
@io.micronaut.el.annotation.ELExpression(value = "${[3,2,1].stream().min((i,j)->i-j).get()}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_31")
@io.micronaut.el.annotation.ELExpression(value = "${[].stream().reduce((l,r)->l+r).orElse(101)}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_32")
@io.micronaut.el.annotation.ELExpression(value = "${[].stream().reduce((l,r)->l+r).orElseGet(()->101)}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_33")
@io.micronaut.el.annotation.ELExpression(value = "${[].stream().sum()}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_34")
@io.micronaut.el.annotation.ELExpression(value = "${[aaa,bbb].get(1)}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_35")
@io.micronaut.el.annotation.ELExpression(value = "${a - b + c}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_36")
@io.micronaut.el.annotation.ELExpression(value = "${a - b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_37")
@io.micronaut.el.annotation.ELExpression(value = "${a / b + c}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_38")
@io.micronaut.el.annotation.ELExpression(value = "${a / b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_39")
@io.micronaut.el.annotation.ELExpression(value = "${a % b + c}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_40")
@io.micronaut.el.annotation.ELExpression(value = "${a % b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_41")
@io.micronaut.el.annotation.ELExpression(value = "${a * b + c}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_42")
@io.micronaut.el.annotation.ELExpression(value = "${a * b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_43")
@io.micronaut.el.annotation.ELExpression(value = "${a + b + c}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_44")
@io.micronaut.el.annotation.ELExpression(value = "${a + b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_45")
@io.micronaut.el.annotation.ELExpression(value = "${a = null; a % b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_46")
@io.micronaut.el.annotation.ELExpression(value = "${a = null; a * b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_47")
@io.micronaut.el.annotation.ELExpression(value = "${a = null; a + b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_48")
@io.micronaut.el.annotation.ELExpression(value = "${a = null; a - b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_49")
@io.micronaut.el.annotation.ELExpression(value = "${a = null; a / b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_50")
@io.micronaut.el.annotation.ELExpression(value = "${a = null; a div b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_51")
@io.micronaut.el.annotation.ELExpression(value = "${a = null; a mod b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_52")
@io.micronaut.el.annotation.ELExpression(value = "${a = null}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_53")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckBigDecimal; a % b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_54")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckBigDecimal; a * b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_55")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckBigDecimal; a + b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_56")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckBigDecimal; a - b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_57")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckBigDecimal; a / b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_58")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckBigDecimal; a div b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_59")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckBigDecimal; a mod b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_60")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckBigDecimal}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_61")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckBigInteger; a % b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_62")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckBigInteger; a * b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_63")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckBigInteger; a + b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_64")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckBigInteger; a - b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_65")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckBigInteger; a / b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_66")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckBigInteger; a div b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_67")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckBigInteger; a mod b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_68")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckBigInteger}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_69")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckByte; a % b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_70")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckByte; a * b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_71")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckByte; a + b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_72")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckByte; a - b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_73")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckByte; a / b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_74")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckByte; a div b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_75")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckByte; a mod b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_76")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckByte}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_77")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckDouble; a - b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_78")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckDouble; a / b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_79")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckDouble; a % b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_80")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckDouble; a * b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_81")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckDouble; a + b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_82")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckDouble; a mod b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_83")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckDouble; a div b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_84")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckDouble}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_85")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckFloat; a % b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_86")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckFloat; a * b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_87")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckFloat; a + b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_88")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckFloat; a - b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_89")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckFloat; a / b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_90")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckFloat; a div b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_91")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckFloat; a mod b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_92")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckFloat}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_93")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckInteger; a % b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_94")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckInteger; a * b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_95")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckInteger; a + b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_96")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckInteger; a - b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_97")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckInteger; a / b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_98")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckInteger; a div b}", expectedType = java.lang.Object.class, name = "EXPRESSION_7_99")
final class TckExpressions6 {
}
