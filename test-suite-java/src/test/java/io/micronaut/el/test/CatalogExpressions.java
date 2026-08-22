package io.micronaut.el.test;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELFunctions;
import io.micronaut.el.annotation.ELVariable;

import java.util.List;

@ELEnvironment(
    variables = {
        @ELVariable(name = "author", type = Author.class),
        @ELVariable(name = "books", type = List.class)
    },
    imports = {Suit.class, Book.class},
    staticImports = Suit.class,
    functions = @ELFunctions(prefix = "fn", value = TextFunctions.class)
)
@ELExpression(value = "${author.name}", name = "authorName")
@ELExpression(value = "${author.greet('Hi')}", name = "greeting")
@ELExpression(value = "${author.books[0].title}", name = "firstTitle")
@ELExpression(value = "${customer.name}", name = "dynamicName")
@ELExpression(value = "${customer.books[0].category}", name = "dynamicCategory")
@ELExpression(value = "${fn:length(author.name)}", name = "nameLength")
@ELExpression(value = "${fn:upper(author.name)}", name = "upperName")
@ELExpression(value = "${Suit.SPADE}", name = "suit")
@ELExpression(value = "${SPADE}", name = "importedSuit")
@ELExpression(value = "${Book('EL', 'history', 10)}", name = "newBook")
@ELExpression(value = "${Boolean.TRUE}", name = "booleanConstant")
@ELExpression(value = "${Integer.valueOf('42')}", name = "staticMethod")
@ELExpression(value = "${books.stream().filter(b->b.unitPrice ge 10).map(b->b.title).toList()}", name = "expensive")
@ELExpression(value = "${books.stream().map(b->b.unitPrice).sum()}", name = "total")
@ELExpression(value = "${books.stream().max((p,q)->p.unitPrice-q.unitPrice).get().title}", name = "mostExpensive")
@ELExpression(value = "${((x,y)->x+y)(3,4)}", name = "immediateLambda")
@ELExpression(value = "${v = (x,y)->x+y; v(3,4)}", name = "assignedLambda")
@ELExpression(value = "${fact = n -> n==0? 1: n*fact(n-1); fact(5)}", name = "factorial")
@ELExpression(value = "${x->y->x+y}", name = "nestedLambda")
public final class CatalogExpressions {

    private CatalogExpressions() {
    }
}
