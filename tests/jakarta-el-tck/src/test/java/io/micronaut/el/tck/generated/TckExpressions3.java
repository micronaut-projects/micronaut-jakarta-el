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
@io.micronaut.el.annotation.ELExpression(value = "${''}", expectedType = java.lang.Integer.class, name = "EXPRESSION_4_0")
@io.micronaut.el.annotation.ELExpression(value = "${''}", expectedType = java.lang.Long.class, name = "EXPRESSION_4_1")
@io.micronaut.el.annotation.ELExpression(value = "${''}", expectedType = java.lang.Short.class, name = "EXPRESSION_4_2")
@io.micronaut.el.annotation.ELExpression(value = "${''}", expectedType = java.math.BigDecimal.class, name = "EXPRESSION_4_3")
@io.micronaut.el.annotation.ELExpression(value = "${''}", expectedType = java.math.BigInteger.class, name = "EXPRESSION_4_4")
@io.micronaut.el.annotation.ELExpression(value = "${'${'}foo}", expectedType = java.lang.String.class, name = "EXPRESSION_4_5")
@io.micronaut.el.annotation.ELExpression(value = "${'1'}", expectedType = java.lang.Byte.class, name = "EXPRESSION_4_6")
@io.micronaut.el.annotation.ELExpression(value = "${'100.5'}", expectedType = java.math.BigDecimal.class, name = "EXPRESSION_4_7")
@io.micronaut.el.annotation.ELExpression(value = "${'125'}", expectedType = java.math.BigInteger.class, name = "EXPRESSION_4_8")
@io.micronaut.el.annotation.ELExpression(value = "${'2'}", expectedType = java.lang.Short.class, name = "EXPRESSION_4_9")
@io.micronaut.el.annotation.ELExpression(value = "${'3'}", expectedType = java.lang.Integer.class, name = "EXPRESSION_4_10")
@io.micronaut.el.annotation.ELExpression(value = "${'30'}", expectedType = java.lang.Byte.class, name = "EXPRESSION_4_11")
@io.micronaut.el.annotation.ELExpression(value = "${'32'}", expectedType = java.lang.Short.class, name = "EXPRESSION_4_12")
@io.micronaut.el.annotation.ELExpression(value = "${'33'}", expectedType = java.lang.Integer.class, name = "EXPRESSION_4_13")
@io.micronaut.el.annotation.ELExpression(value = "${'34'}", expectedType = java.lang.Long.class, name = "EXPRESSION_4_14")
@io.micronaut.el.annotation.ELExpression(value = "${'35.5'}", expectedType = java.lang.Float.class, name = "EXPRESSION_4_15")
@io.micronaut.el.annotation.ELExpression(value = "${'36.5'}", expectedType = java.lang.Double.class, name = "EXPRESSION_4_16")
@io.micronaut.el.annotation.ELExpression(value = "${'4'}", expectedType = java.lang.Long.class, name = "EXPRESSION_4_17")
@io.micronaut.el.annotation.ELExpression(value = "${'5'}", expectedType = java.lang.Float.class, name = "EXPRESSION_4_18")
@io.micronaut.el.annotation.ELExpression(value = "${'6'}", expectedType = java.lang.Double.class, name = "EXPRESSION_4_19")
@io.micronaut.el.annotation.ELExpression(value = "${'7'}", expectedType = java.math.BigInteger.class, name = "EXPRESSION_4_20")
@io.micronaut.el.annotation.ELExpression(value = "${'8'}", expectedType = java.math.BigDecimal.class, name = "EXPRESSION_4_21")
@io.micronaut.el.annotation.ELExpression(value = "${'A'}", expectedType = java.lang.Byte.class, name = "EXPRESSION_4_22")
@io.micronaut.el.annotation.ELExpression(value = "${'A'}", expectedType = java.lang.Double.class, name = "EXPRESSION_4_23")
@io.micronaut.el.annotation.ELExpression(value = "${'A'}", expectedType = java.lang.Float.class, name = "EXPRESSION_4_24")
@io.micronaut.el.annotation.ELExpression(value = "${'A'}", expectedType = java.lang.Integer.class, name = "EXPRESSION_4_25")
@io.micronaut.el.annotation.ELExpression(value = "${'A'}", expectedType = java.lang.Long.class, name = "EXPRESSION_4_26")
@io.micronaut.el.annotation.ELExpression(value = "${'A'}", expectedType = java.lang.Short.class, name = "EXPRESSION_4_27")
@io.micronaut.el.annotation.ELExpression(value = "${'A'}", expectedType = java.math.BigDecimal.class, name = "EXPRESSION_4_28")
@io.micronaut.el.annotation.ELExpression(value = "${'A'}", expectedType = java.math.BigInteger.class, name = "EXPRESSION_4_29")
@io.micronaut.el.annotation.ELExpression(value = "${'STRING'}", expectedType = java.lang.Character.class, name = "EXPRESSION_4_30")
@io.micronaut.el.annotation.ELExpression(value = "${'\\'pullstring\\''}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_31")
@io.micronaut.el.annotation.ELExpression(value = "${'myKey'}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_32")
@io.micronaut.el.annotation.ELExpression(value = "${'myValue'}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_33")
@io.micronaut.el.annotation.ELExpression(value = "${'myValueA'}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_34")
@io.micronaut.el.annotation.ELExpression(value = "${'myValueB'}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_35")
@io.micronaut.el.annotation.ELExpression(value = "${'str\\\\ing'}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_36")
@io.micronaut.el.annotation.ELExpression(value = "${'string'}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_37")
@io.micronaut.el.annotation.ELExpression(value = "${(((x, y)-> x % y)(a, b))}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_38")
@io.micronaut.el.annotation.ELExpression(value = "${(((x, y)-> x * y)(a, b))}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_39")
@io.micronaut.el.annotation.ELExpression(value = "${(((x, y)-> x + y)(a, b))}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_40")
@io.micronaut.el.annotation.ELExpression(value = "${(((x, y)-> x += y)(a, b))}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_41")
@io.micronaut.el.annotation.ELExpression(value = "${(((x, y)-> x - y)(a, b))}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_42")
@io.micronaut.el.annotation.ELExpression(value = "${(((x, y)-> x / y)(a, b))}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_43")
@io.micronaut.el.annotation.ELExpression(value = "${(((x, y)-> x div y)(a, b))}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_44")
@io.micronaut.el.annotation.ELExpression(value = "${(((x, y)-> x mod y)(a, b))}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_45")
@io.micronaut.el.annotation.ELExpression(value = "${(()->y->y % a)()(b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_46")
@io.micronaut.el.annotation.ELExpression(value = "${(()->y->y * a)()(b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_47")
@io.micronaut.el.annotation.ELExpression(value = "${(()->y->y + a)()(b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_48")
@io.micronaut.el.annotation.ELExpression(value = "${(()->y->y - a)()(b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_49")
@io.micronaut.el.annotation.ELExpression(value = "${(()->y->y / a)()(b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_50")
@io.micronaut.el.annotation.ELExpression(value = "${(()->y->y div a)()(b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_51")
@io.micronaut.el.annotation.ELExpression(value = "${(()->y->y mod a)()(b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_52")
@io.micronaut.el.annotation.ELExpression(value = "${(1 + 5) * 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_53")
@io.micronaut.el.annotation.ELExpression(value = "${(1 - 5) + 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_54")
@io.micronaut.el.annotation.ELExpression(value = "${(2 + 3) - 10}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_55")
@io.micronaut.el.annotation.ELExpression(value = "${(2 + 7) % 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_56")
@io.micronaut.el.annotation.ELExpression(value = "${(4 + 4) / 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_57")
@io.micronaut.el.annotation.ELExpression(value = "${(cond->false? a % 2: a % b)(a)}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_58")
@io.micronaut.el.annotation.ELExpression(value = "${(cond->false? a * 2: a * b)(a)}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_59")
@io.micronaut.el.annotation.ELExpression(value = "${(cond->false? a + 2: a + b)(a)}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_60")
@io.micronaut.el.annotation.ELExpression(value = "${(cond->false? a - 2: a - b)(a)}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_61")
@io.micronaut.el.annotation.ELExpression(value = "${(cond->false? a / 2: a / b)(a)}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_62")
@io.micronaut.el.annotation.ELExpression(value = "${(cond->false? a div 2: a div b)(a)}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_63")
@io.micronaut.el.annotation.ELExpression(value = "${(cond->false? a mod 2: a mod b)(a)}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_64")
@io.micronaut.el.annotation.ELExpression(value = "${(cond->true? a % b: a % 2)(a)}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_65")
@io.micronaut.el.annotation.ELExpression(value = "${(cond->true? a * b: a * 2)(a)}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_66")
@io.micronaut.el.annotation.ELExpression(value = "${(cond->true? a + b: a + 2)(a)}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_67")
@io.micronaut.el.annotation.ELExpression(value = "${(cond->true? a - b: a - 2)(a)}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_68")
@io.micronaut.el.annotation.ELExpression(value = "${(cond->true? a / b: a / 2)(a)}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_69")
@io.micronaut.el.annotation.ELExpression(value = "${(cond->true? a div b: a div 2)(a)}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_70")
@io.micronaut.el.annotation.ELExpression(value = "${(cond->true? a mod b: a mod 2)(a)}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_71")
@io.micronaut.el.annotation.ELExpression(value = "${(x->(y->x % y)(a))(b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_72")
@io.micronaut.el.annotation.ELExpression(value = "${(x->(y->x * y)(a))(b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_73")
@io.micronaut.el.annotation.ELExpression(value = "${(x->(y->x + y)(a))(b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_74")
@io.micronaut.el.annotation.ELExpression(value = "${(x->(y->x - y)(a))(b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_75")
@io.micronaut.el.annotation.ELExpression(value = "${(x->(y->x / y)(a))(b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_76")
@io.micronaut.el.annotation.ELExpression(value = "${(x->(y->x div y)(a))(b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_77")
@io.micronaut.el.annotation.ELExpression(value = "${(x->(y->x mod y)(a))(b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_78")
@io.micronaut.el.annotation.ELExpression(value = "${-0.72}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_79")
@io.micronaut.el.annotation.ELExpression(value = "${-0.003444}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_80")
@io.micronaut.el.annotation.ELExpression(value = "${-1.0}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_81")
@io.micronaut.el.annotation.ELExpression(value = "${-10.0}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_82")
@io.micronaut.el.annotation.ELExpression(value = "${-2147483647}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_83")
@io.micronaut.el.annotation.ELExpression(value = "${-2}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_84")
@io.micronaut.el.annotation.ELExpression(value = "${-344400.0}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_85")
@io.micronaut.el.annotation.ELExpression(value = "${-34.44}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_86")
@io.micronaut.el.annotation.ELExpression(value = "${-70.2}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_87")
@io.micronaut.el.annotation.ELExpression(value = "${-A}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_88")
@io.micronaut.el.annotation.ELExpression(value = "${-null}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_89")
@io.micronaut.el.annotation.ELExpression(value = "${0.999}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_90")
@io.micronaut.el.annotation.ELExpression(value = "${1 - 4 / 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_91")
@io.micronaut.el.annotation.ELExpression(value = "${1 - 4 div 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_92")
@io.micronaut.el.annotation.ELExpression(value = "${1 - 5 * 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_93")
@io.micronaut.el.annotation.ELExpression(value = "${1 - 7 % 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_94")
@io.micronaut.el.annotation.ELExpression(value = "${1 - 7 mod 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_95")
@io.micronaut.el.annotation.ELExpression(value = "${1 != nullValue}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_96")
@io.micronaut.el.annotation.ELExpression(value = "${1 + 4 / 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_97")
@io.micronaut.el.annotation.ELExpression(value = "${1 + 4 div 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_98")
@io.micronaut.el.annotation.ELExpression(value = "${1 + 5 * 2}", expectedType = java.lang.Object.class, name = "EXPRESSION_4_99")
final class TckExpressions3 {
}
