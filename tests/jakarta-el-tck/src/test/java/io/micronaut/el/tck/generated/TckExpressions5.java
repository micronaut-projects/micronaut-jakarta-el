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
@io.micronaut.el.annotation.ELExpression(value = "${20 div 2 == 10 ? false : true}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_0")
@io.micronaut.el.annotation.ELExpression(value = "${2000}", expectedType = java.lang.Integer.class, name = "EXPRESSION_6_1")
@io.micronaut.el.annotation.ELExpression(value = "${21 % 2 == 1 ? false : true}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_2")
@io.micronaut.el.annotation.ELExpression(value = "${21 mod 2 == 1 ? false : true}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_3")
@io.micronaut.el.annotation.ELExpression(value = "${2147483647}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_4")
@io.micronaut.el.annotation.ELExpression(value = "${23400.0}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_5")
@io.micronaut.el.annotation.ELExpression(value = "${24 / 2 == 10 ? false : true}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_6")
@io.micronaut.el.annotation.ELExpression(value = "${24 div 2 == 10 ? false : true}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_7")
@io.micronaut.el.annotation.ELExpression(value = "${2e+3}", expectedType = java.lang.Float.class, name = "EXPRESSION_6_8")
@io.micronaut.el.annotation.ELExpression(value = "${2}", expectedType = java.lang.Byte.class, name = "EXPRESSION_6_9")
@io.micronaut.el.annotation.ELExpression(value = "${2}", expectedType = java.lang.Character.class, name = "EXPRESSION_6_10")
@io.micronaut.el.annotation.ELExpression(value = "${3 % 2 == 1}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_11")
@io.micronaut.el.annotation.ELExpression(value = "${3 % 8 gt 1}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_12")
@io.micronaut.el.annotation.ELExpression(value = "${3 * 2 < 8}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_13")
@io.micronaut.el.annotation.ELExpression(value = "${3 > 4  / 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_14")
@io.micronaut.el.annotation.ELExpression(value = "${3 > 4  div 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_15")
@io.micronaut.el.annotation.ELExpression(value = "${3 mod 8 gt 1}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_16")
@io.micronaut.el.annotation.ELExpression(value = "${3 mod 2 == 1}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_17")
@io.micronaut.el.annotation.ELExpression(value = "${33}", expectedType = java.lang.Byte.class, name = "EXPRESSION_6_18")
@io.micronaut.el.annotation.ELExpression(value = "${33}", expectedType = java.lang.Double.class, name = "EXPRESSION_6_19")
@io.micronaut.el.annotation.ELExpression(value = "${33}", expectedType = java.lang.Float.class, name = "EXPRESSION_6_20")
@io.micronaut.el.annotation.ELExpression(value = "${33}", expectedType = java.lang.Integer.class, name = "EXPRESSION_6_21")
@io.micronaut.el.annotation.ELExpression(value = "${33}", expectedType = java.lang.Long.class, name = "EXPRESSION_6_22")
@io.micronaut.el.annotation.ELExpression(value = "${33}", expectedType = java.lang.Short.class, name = "EXPRESSION_6_23")
@io.micronaut.el.annotation.ELExpression(value = "${33}", expectedType = java.math.BigDecimal.class, name = "EXPRESSION_6_24")
@io.micronaut.el.annotation.ELExpression(value = "${33}", expectedType = java.math.BigInteger.class, name = "EXPRESSION_6_25")
@io.micronaut.el.annotation.ELExpression(value = "${30}", expectedType = java.lang.Byte.class, name = "EXPRESSION_6_26")
@io.micronaut.el.annotation.ELExpression(value = "${30}", expectedType = java.lang.Double.class, name = "EXPRESSION_6_27")
@io.micronaut.el.annotation.ELExpression(value = "${30}", expectedType = java.lang.Float.class, name = "EXPRESSION_6_28")
@io.micronaut.el.annotation.ELExpression(value = "${30}", expectedType = java.lang.Integer.class, name = "EXPRESSION_6_29")
@io.micronaut.el.annotation.ELExpression(value = "${30}", expectedType = java.lang.Long.class, name = "EXPRESSION_6_30")
@io.micronaut.el.annotation.ELExpression(value = "${30}", expectedType = java.lang.Short.class, name = "EXPRESSION_6_31")
@io.micronaut.el.annotation.ELExpression(value = "${30}", expectedType = java.math.BigDecimal.class, name = "EXPRESSION_6_32")
@io.micronaut.el.annotation.ELExpression(value = "${30}", expectedType = java.math.BigInteger.class, name = "EXPRESSION_6_33")
@io.micronaut.el.annotation.ELExpression(value = "${32}", expectedType = java.lang.Byte.class, name = "EXPRESSION_6_34")
@io.micronaut.el.annotation.ELExpression(value = "${32}", expectedType = java.lang.Double.class, name = "EXPRESSION_6_35")
@io.micronaut.el.annotation.ELExpression(value = "${32}", expectedType = java.lang.Float.class, name = "EXPRESSION_6_36")
@io.micronaut.el.annotation.ELExpression(value = "${32}", expectedType = java.lang.Integer.class, name = "EXPRESSION_6_37")
@io.micronaut.el.annotation.ELExpression(value = "${32}", expectedType = java.lang.Long.class, name = "EXPRESSION_6_38")
@io.micronaut.el.annotation.ELExpression(value = "${32}", expectedType = java.lang.Short.class, name = "EXPRESSION_6_39")
@io.micronaut.el.annotation.ELExpression(value = "${32}", expectedType = java.math.BigDecimal.class, name = "EXPRESSION_6_40")
@io.micronaut.el.annotation.ELExpression(value = "${32}", expectedType = java.math.BigInteger.class, name = "EXPRESSION_6_41")
@io.micronaut.el.annotation.ELExpression(value = "${34}", expectedType = java.lang.Byte.class, name = "EXPRESSION_6_42")
@io.micronaut.el.annotation.ELExpression(value = "${34}", expectedType = java.lang.Double.class, name = "EXPRESSION_6_43")
@io.micronaut.el.annotation.ELExpression(value = "${34}", expectedType = java.lang.Float.class, name = "EXPRESSION_6_44")
@io.micronaut.el.annotation.ELExpression(value = "${34}", expectedType = java.lang.Integer.class, name = "EXPRESSION_6_45")
@io.micronaut.el.annotation.ELExpression(value = "${34}", expectedType = java.lang.Long.class, name = "EXPRESSION_6_46")
@io.micronaut.el.annotation.ELExpression(value = "${34}", expectedType = java.lang.Short.class, name = "EXPRESSION_6_47")
@io.micronaut.el.annotation.ELExpression(value = "${34}", expectedType = java.math.BigDecimal.class, name = "EXPRESSION_6_48")
@io.micronaut.el.annotation.ELExpression(value = "${34}", expectedType = java.math.BigInteger.class, name = "EXPRESSION_6_49")
@io.micronaut.el.annotation.ELExpression(value = "${35.5}", expectedType = java.lang.Byte.class, name = "EXPRESSION_6_50")
@io.micronaut.el.annotation.ELExpression(value = "${35.5}", expectedType = java.lang.Double.class, name = "EXPRESSION_6_51")
@io.micronaut.el.annotation.ELExpression(value = "${35.5}", expectedType = java.lang.Float.class, name = "EXPRESSION_6_52")
@io.micronaut.el.annotation.ELExpression(value = "${35.5}", expectedType = java.lang.Integer.class, name = "EXPRESSION_6_53")
@io.micronaut.el.annotation.ELExpression(value = "${35.5}", expectedType = java.lang.Long.class, name = "EXPRESSION_6_54")
@io.micronaut.el.annotation.ELExpression(value = "${35.5}", expectedType = java.lang.Short.class, name = "EXPRESSION_6_55")
@io.micronaut.el.annotation.ELExpression(value = "${35.5}", expectedType = java.math.BigDecimal.class, name = "EXPRESSION_6_56")
@io.micronaut.el.annotation.ELExpression(value = "${35.5}", expectedType = java.math.BigInteger.class, name = "EXPRESSION_6_57")
@io.micronaut.el.annotation.ELExpression(value = "${36.5}", expectedType = java.lang.Byte.class, name = "EXPRESSION_6_58")
@io.micronaut.el.annotation.ELExpression(value = "${36.5}", expectedType = java.lang.Double.class, name = "EXPRESSION_6_59")
@io.micronaut.el.annotation.ELExpression(value = "${36.5}", expectedType = java.lang.Float.class, name = "EXPRESSION_6_60")
@io.micronaut.el.annotation.ELExpression(value = "${36.5}", expectedType = java.lang.Integer.class, name = "EXPRESSION_6_61")
@io.micronaut.el.annotation.ELExpression(value = "${36.5}", expectedType = java.lang.Long.class, name = "EXPRESSION_6_62")
@io.micronaut.el.annotation.ELExpression(value = "${36.5}", expectedType = java.lang.Short.class, name = "EXPRESSION_6_63")
@io.micronaut.el.annotation.ELExpression(value = "${36.5}", expectedType = java.math.BigDecimal.class, name = "EXPRESSION_6_64")
@io.micronaut.el.annotation.ELExpression(value = "${36.5}", expectedType = java.math.BigInteger.class, name = "EXPRESSION_6_65")
@io.micronaut.el.annotation.ELExpression(value = "${3}", expectedType = java.lang.Character.class, name = "EXPRESSION_6_66")
@io.micronaut.el.annotation.ELExpression(value = "${testPredicateLong(x -> x.compareTo('1234') == 0)}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_67")
@io.micronaut.el.annotation.ELExpression(value = "${testPredicateLong(x -> x.compareTo('data') == 0)}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_68")
@io.micronaut.el.annotation.ELExpression(value = "${testPredicateString('notLambdaExpression')}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_69")
@io.micronaut.el.annotation.ELExpression(value = "${testPredicateString(x -> x.compareTo(1234) == 0)}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_70")
@io.micronaut.el.annotation.ELExpression(value = "${testPredicateString(x -> x.equals('other'))}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_71")
@io.micronaut.el.annotation.ELExpression(value = "${testPredicateString(x -> x.equals('data'))}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_72")
@io.micronaut.el.annotation.ELExpression(value = "${testPrimitiveBooleanArray([\"true\", false, true, 'false', null, \"\"].toArray())}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_73")
@io.micronaut.el.annotation.ELExpression(value = "${testPrimitiveBooleanArray(['true', 'false', 1234].toArray())}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_74")
@io.micronaut.el.annotation.ELExpression(value = "${testPrimitiveBooleanArray([true, false, true, false, true])}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_75")
@io.micronaut.el.annotation.ELExpression(value = "${testPrimitiveBooleanArray([true, false].toArray())}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_76")
@io.micronaut.el.annotation.ELExpression(value = "${testPrimitiveBooleanArray(null)}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_77")
@io.micronaut.el.annotation.ELExpression(value = "${true}", expectedType = java.lang.Boolean.class, name = "EXPRESSION_6_78")
@io.micronaut.el.annotation.ELExpression(value = "${true}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_79")
@io.micronaut.el.annotation.ELExpression(value = "${worker.firstName}", expectedType = java.lang.String.class, name = "EXPRESSION_6_80")
@io.micronaut.el.annotation.ELExpression(value = "${worker['firstName']}", expectedType = java.lang.String.class, name = "EXPRESSION_6_81")
@io.micronaut.el.annotation.ELExpression(value = "${z = (x,y)->x % y}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_82")
@io.micronaut.el.annotation.ELExpression(value = "${z = (x,y)->x * y}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_83")
@io.micronaut.el.annotation.ELExpression(value = "${z = (x,y)->x + y}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_84")
@io.micronaut.el.annotation.ELExpression(value = "${z = (x,y)->x - y}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_85")
@io.micronaut.el.annotation.ELExpression(value = "${z = (x,y)->x / y}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_86")
@io.micronaut.el.annotation.ELExpression(value = "${z = (x,y)->x div y}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_87")
@io.micronaut.el.annotation.ELExpression(value = "${z = (x,y)->x mod y}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_88")
@io.micronaut.el.annotation.ELExpression(value = "${z(a, b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_89")
@io.micronaut.el.annotation.ELExpression(value = "${{aaa,bbb}.contains(aaa)}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_90")
@io.micronaut.el.annotation.ELExpression(value = "${{aaa:bbb}.get(aaa)}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_91")
@io.micronaut.el.annotation.ELExpression(value = "${A - B}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_92")
@io.micronaut.el.annotation.ELExpression(value = "${A != B}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_93")
@io.micronaut.el.annotation.ELExpression(value = "${A + B}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_94")
@io.micronaut.el.annotation.ELExpression(value = "${A == B}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_95")
@io.micronaut.el.annotation.ELExpression(value = "${A >= B}", expectedType = java.lang.Boolean.class, name = "EXPRESSION_6_96")
@io.micronaut.el.annotation.ELExpression(value = "${A >= B}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_97")
@io.micronaut.el.annotation.ELExpression(value = "${A > B}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_98")
@io.micronaut.el.annotation.ELExpression(value = "${A ?B: C}", expectedType = java.lang.Object.class, name = "EXPRESSION_6_99")
final class TckExpressions5 {
}
