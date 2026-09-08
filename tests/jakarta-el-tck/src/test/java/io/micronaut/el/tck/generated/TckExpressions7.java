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
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckInteger; a mod b}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_0")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckInteger}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_1")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckLong}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_2")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckLong; a % b}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_3")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckLong; a * b}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_4")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckLong; a + b}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_5")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckLong; a - b}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_6")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckLong; a / b}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_7")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckLong; a div b}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_8")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckLong; a mod b}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_9")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckNull}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_10")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckShort; a % b}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_11")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckShort; a * b}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_12")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckShort; a + b}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_13")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckShort; a - b}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_14")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckShort; a / b}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_15")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckShort; a div b}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_16")
@io.micronaut.el.annotation.ELExpression(value = "${a = types.tckShort}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_17")
@io.micronaut.el.annotation.ELExpression(value = "${a mod b + c}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_18")
@io.micronaut.el.annotation.ELExpression(value = "${a mod b}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_19")
@io.micronaut.el.annotation.ELExpression(value = "${a div b + c}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_20")
@io.micronaut.el.annotation.ELExpression(value = "${a div b}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_21")
@io.micronaut.el.annotation.ELExpression(value = "${a='Testing'}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_22")
@io.micronaut.el.annotation.ELExpression(value = "${a=types.tckByte}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_23")
@io.micronaut.el.annotation.ELExpression(value = "${a=types.tckBigDecimal}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_24")
@io.micronaut.el.annotation.ELExpression(value = "${a=types.tckBigInteger}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_25")
@io.micronaut.el.annotation.ELExpression(value = "${a=types.tckDouble}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_26")
@io.micronaut.el.annotation.ELExpression(value = "${a=types.tckFloat}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_27")
@io.micronaut.el.annotation.ELExpression(value = "${a=types.tckInteger}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_28")
@io.micronaut.el.annotation.ELExpression(value = "${a=types.tckLong}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_29")
@io.micronaut.el.annotation.ELExpression(value = "${a=types.tckShort}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_30")
@io.micronaut.el.annotation.ELExpression(value = "${b = null}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_31")
@io.micronaut.el.annotation.ELExpression(value = "${b = types.tckBigDecimal}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_32")
@io.micronaut.el.annotation.ELExpression(value = "${b = types.tckBigInteger}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_33")
@io.micronaut.el.annotation.ELExpression(value = "${b = types.tckByte}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_34")
@io.micronaut.el.annotation.ELExpression(value = "${b = types.tckDouble}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_35")
@io.micronaut.el.annotation.ELExpression(value = "${b = types.tckFloat}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_36")
@io.micronaut.el.annotation.ELExpression(value = "${b = types.tckInteger}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_37")
@io.micronaut.el.annotation.ELExpression(value = "${b = types.tckLong}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_38")
@io.micronaut.el.annotation.ELExpression(value = "${b = types.tckNull}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_39")
@io.micronaut.el.annotation.ELExpression(value = "${b = types.tckShort}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_40")
@io.micronaut.el.annotation.ELExpression(value = "${b='Testing'}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_41")
@io.micronaut.el.annotation.ELExpression(value = "${bar}", expectedType = java.lang.String.class, name = "EXPRESSION_8_42")
@io.micronaut.el.annotation.ELExpression(value = "${c = 0; [1,2,3,4,5,6].stream().reduce(0, (l,r)->(c = c+1; c % 2 == 0? l+r: l-r))}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_43")
@io.micronaut.el.annotation.ELExpression(value = "${c = types.tckBigDecimal}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_44")
@io.micronaut.el.annotation.ELExpression(value = "${comparing = map->(x,y)->map(x).compareTo(map(y))}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_45")
@io.micronaut.el.annotation.ELExpression(value = "${cond = false}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_46")
@io.micronaut.el.annotation.ELExpression(value = "${cond = true}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_47")
@io.micronaut.el.annotation.ELExpression(value = "${customers.stream().filter(c->c.country=='USA').flatMap(c->c.orders.stream()).toList()}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_48")
@io.micronaut.el.annotation.ELExpression(value = "${customers.stream().max((x,y)->x.orders.size()-y.orders.size()).get().name}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_49")
@io.micronaut.el.annotation.ELExpression(value = "${customers.stream().max(comparing(c->c.orders.size())).get().name}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_50")
@io.micronaut.el.annotation.ELExpression(value = "${customers.stream().min((x,y)->x.orders.size()-y.orders.size()).get().name}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_51")
@io.micronaut.el.annotation.ELExpression(value = "${customers.stream().min(comparing(c->c.orders.size())).get().name}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_52")
@io.micronaut.el.annotation.ELExpression(value = "${empty A}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_53")
@io.micronaut.el.annotation.ELExpression(value = "${empty null}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_54")
@io.micronaut.el.annotation.ELExpression(value = "${employee.firstname}${employee.lastname}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_55")
@io.micronaut.el.annotation.ELExpression(value = "${employee.lastname}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_56")
@io.micronaut.el.annotation.ELExpression(value = "${f = ()->y->y % a; f()(b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_57")
@io.micronaut.el.annotation.ELExpression(value = "${f = ()->y->y * a; f()(b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_58")
@io.micronaut.el.annotation.ELExpression(value = "${f = ()->y->y + a; f()(b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_59")
@io.micronaut.el.annotation.ELExpression(value = "${f = ()->y->y - a; f()(b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_60")
@io.micronaut.el.annotation.ELExpression(value = "${f = ()->y->y / a; f()(b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_61")
@io.micronaut.el.annotation.ELExpression(value = "${f = ()->y->y div a; f()(b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_62")
@io.micronaut.el.annotation.ELExpression(value = "${f = ()->y->y mod a; f()(b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_63")
@io.micronaut.el.annotation.ELExpression(value = "${f = (x)->(tem=x; y->tem % y); f(a)(b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_64")
@io.micronaut.el.annotation.ELExpression(value = "${f = (x)->(tem=x; y->tem * y); f(a)(b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_65")
@io.micronaut.el.annotation.ELExpression(value = "${f = (x)->(tem=x; y->tem + y); f(a)(b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_66")
@io.micronaut.el.annotation.ELExpression(value = "${f = (x)->(tem=x; y->tem - y); f(a)(b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_67")
@io.micronaut.el.annotation.ELExpression(value = "${f = (x)->(tem=x; y->tem / y); f(a)(b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_68")
@io.micronaut.el.annotation.ELExpression(value = "${f = (x)->(tem=x; y->tem div y); f(a)(b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_69")
@io.micronaut.el.annotation.ELExpression(value = "${f = (x)->(tem=x; y->tem mod y); f(a)(b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_70")
@io.micronaut.el.annotation.ELExpression(value = "${false}", expectedType = java.lang.Byte.class, name = "EXPRESSION_8_71")
@io.micronaut.el.annotation.ELExpression(value = "${false}", expectedType = java.lang.Character.class, name = "EXPRESSION_8_72")
@io.micronaut.el.annotation.ELExpression(value = "${false}", expectedType = java.lang.Double.class, name = "EXPRESSION_8_73")
@io.micronaut.el.annotation.ELExpression(value = "${false}", expectedType = java.lang.Float.class, name = "EXPRESSION_8_74")
@io.micronaut.el.annotation.ELExpression(value = "${false}", expectedType = java.lang.Integer.class, name = "EXPRESSION_8_75")
@io.micronaut.el.annotation.ELExpression(value = "${false}", expectedType = java.lang.Long.class, name = "EXPRESSION_8_76")
@io.micronaut.el.annotation.ELExpression(value = "${false}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_77")
@io.micronaut.el.annotation.ELExpression(value = "${false}", expectedType = java.lang.Short.class, name = "EXPRESSION_8_78")
@io.micronaut.el.annotation.ELExpression(value = "${false}", expectedType = java.lang.String.class, name = "EXPRESSION_8_79")
@io.micronaut.el.annotation.ELExpression(value = "${false}", expectedType = java.math.BigDecimal.class, name = "EXPRESSION_8_80")
@io.micronaut.el.annotation.ELExpression(value = "${false}", expectedType = java.math.BigInteger.class, name = "EXPRESSION_8_81")
@io.micronaut.el.annotation.ELExpression(value = "${foo}", expectedType = java.lang.String.class, name = "EXPRESSION_8_82")
@io.micronaut.el.annotation.ELExpression(value = "${func = (x,y)->x % y; func(a, b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_83")
@io.micronaut.el.annotation.ELExpression(value = "${func = (x,y)->x * y; func(a, b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_84")
@io.micronaut.el.annotation.ELExpression(value = "${func = (x,y)->x + y; func(a, b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_85")
@io.micronaut.el.annotation.ELExpression(value = "${func = (x,y)->x - y; func(a, b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_86")
@io.micronaut.el.annotation.ELExpression(value = "${func = (x,y)->x / y; func(a, b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_87")
@io.micronaut.el.annotation.ELExpression(value = "${func = (x,y)->x div y; func(a, b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_88")
@io.micronaut.el.annotation.ELExpression(value = "${func = (x,y)->x mod y; func(a, b)}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_89")
@io.micronaut.el.annotation.ELExpression(value = "${ints.stream().average().get()}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_90")
@io.micronaut.el.annotation.ELExpression(value = "${javabook}", expectedType = java.lang.String.class, name = "EXPRESSION_8_91")
@io.micronaut.el.annotation.ELExpression(value = "${lst = []; [1,2,3,4].stream().peek(i->lst.add(i)).toList()}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_92")
@io.micronaut.el.annotation.ELExpression(value = "${lst = []; products.stream().forEach(p->lst.add(p.name)); lst}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_93")
@io.micronaut.el.annotation.ELExpression(value = "${lst.stream().toList()}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_94")
@io.micronaut.el.annotation.ELExpression(value = "${not A}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_95")
@io.micronaut.el.annotation.ELExpression(value = "${null}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_96")
@io.micronaut.el.annotation.ELExpression(value = "${products.stream().filter(p->p.unitPrice >= 10 && p.unitPrice < 12).sorted((p,q)->p.unitPrice-q.unitPrice).map(p->[p.name,p.unitPrice]).toList()}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_97")
@io.micronaut.el.annotation.ELExpression(value = "${products.stream().sorted((p,q)->p.unitPrice-q.unitPrice).limit(1).toList()}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_98")
@io.micronaut.el.annotation.ELExpression(value = "${products.stream().sorted((p,q)->p.unitPrice-q.unitPrice).limit(2).toList()}", expectedType = java.lang.Object.class, name = "EXPRESSION_8_99")
final class TckExpressions7 {
}
