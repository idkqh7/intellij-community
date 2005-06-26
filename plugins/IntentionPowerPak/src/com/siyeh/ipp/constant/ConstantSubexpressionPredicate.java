package com.siyeh.ipp.constant;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.IncorrectOperationException;
import com.siyeh.ipp.base.PsiElementPredicate;
import org.jetbrains.annotations.Nullable;

class ConstantSubexpressionPredicate implements PsiElementPredicate{
    public boolean satisfiedBy(PsiElement element){
        if(!(element instanceof PsiJavaToken) &&
                !(element.getPrevSibling() instanceof PsiJavaToken)){
            return false;
        }
        final PsiElement parent = element.getParent();
        if(!(parent instanceof PsiBinaryExpression)){
            return false;
        }
        final PsiBinaryExpression binaryExpression = (PsiBinaryExpression) parent;
        final PsiBinaryExpression subexpression = getSubexpression(binaryExpression);
        if(subexpression == null)
        {
            return false;
        }
        if(binaryExpression.equals(subexpression) &&
                !isPartOfConstantExpression(binaryExpression)){
            // handled by ConstantExpressonIntention
            return false;
        }
        final PsiType type = binaryExpression.getType();
        if(type == null || type.equalsToText("java.lang.String")){
            // handled by JoinConcatenatedStringLiteralsIntention
            return false;
        }
        if(!PsiUtil.isConstantExpression(subexpression)){
            return false;
        }
        final PsiManager manager = element.getManager();
        final PsiConstantEvaluationHelper helper =
                manager.getConstantEvaluationHelper();
        final Object value = helper.computeConstantExpression(subexpression);
        return value != null;
    }

    private static boolean isPartOfConstantExpression(PsiBinaryExpression binaryExpression){
        final PsiElement containingElement = binaryExpression.getParent();
        if(containingElement instanceof PsiExpression){
            final PsiExpression containingExpression = (PsiExpression) containingElement;
            if(!PsiUtil.isConstantExpression(containingExpression)){
                return false;
            }
        } else{
            return false;
        }
        return true;
    }

    /**
     * Returns the smallest subexpression (if precendence allows it). example:
     * variable + 2 + 3 normally gets evaluated left to right -> (variable + 2)
     * + 3 this method returns the right most legal subexpression -> 2 + 3
     */
    @Nullable
    private static PsiBinaryExpression getSubexpression(PsiBinaryExpression expression){
        final PsiBinaryExpression binaryExpression = (PsiBinaryExpression) expression.copy();
        final PsiExpression rhs = binaryExpression.getROperand();
        if(rhs == null){
            return null;
        }
        final PsiExpression lhs = binaryExpression.getLOperand();
        if(!(lhs instanceof PsiBinaryExpression)){
            return expression;
        }
        final PsiBinaryExpression lhsBinaryExpression = (PsiBinaryExpression) lhs;
        final PsiExpression leftSide = lhsBinaryExpression.getROperand();
        if(leftSide == null)
        {
           return null;
        }
        try{
            lhs.replace(leftSide);
        } catch(IncorrectOperationException ignore){
            return null;
        }
        return binaryExpression;
    }
}