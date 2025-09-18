/*
 * 文件名称:          LayoutKit.java
 *  
 * 编译器:            android2.2
 * 时间:              下午4:16:55
 */
package com.reader.office.wp.view;

import com.reader.office.constant.MainConstant;
import com.reader.office.constant.wp.AttrIDConstant;
import com.reader.office.constant.wp.WPViewConstant;
import com.reader.office.simpletext.font.FontKit;
import com.reader.office.simpletext.model.AttrManage;
import com.reader.office.simpletext.model.IDocument;
import com.reader.office.simpletext.model.IElement;
import com.reader.office.simpletext.view.DocAttr;
import com.reader.office.simpletext.view.IView;
import com.reader.office.simpletext.view.PageAttr;
import com.reader.office.simpletext.view.ParaAttr;
import com.reader.office.simpletext.view.ViewKit;
import com.reader.office.system.IControl;
import com.reader.office.wp.control.Word;


/**
 * 布局工具类
 * <p>
 * <p>
 * Read版本:        Read V1.0
 * <p>
 * 作者:            ljj8494
 * <p>
 * 日期:            2011-12-13
 * <p>
 * 负责人:          ljj8494
 * <p>
 * 负责小组:         
 * <p>
 * <p>
 */
public class LayoutKit
{
    //
    private static LayoutKit kit = new LayoutKit();

    private LayoutKit()
    {   
    }
    /**
     * 
     * @return
     */
    public static LayoutKit instance()
    {
        return kit;
    }
    
    /**
     * 布局页面坐标
     * @param root
     * @param zoom
     */
    public void layoutAllPage(PageRoot root, float zoom)
    {
        if (root == null || root.getChildView() == null)
        {
            return;
        }
        Word word = (Word)root.getContainer();
        int dx = WPViewConstant.PAGE_SPACE;
        int dy = WPViewConstant.PAGE_SPACE;
        IView pv = root.getChildView();
        int width = pv.getWidth();
        int visibleWidth = word.getWidth();
        visibleWidth = visibleWidth == 0 ? word.getWordWidth() : visibleWidth;
        if (visibleWidth > width * zoom)
        {
            dx += (int)(visibleWidth / zoom - width - WPViewConstant.PAGE_SPACE * 2) / 2;
        }
        while (pv != null)
        {
            pv.setLocation(dx, dy);
            dy += pv.getHeight() + WPViewConstant.PAGE_SPACE;
            pv = pv.getNextView();
        }
        root.setSize(width + WPViewConstant.PAGE_SPACE * 2, dy);
        ((Word)root.getContainer()).setSize(width + WPViewConstant.PAGE_SPACE * 2, dy);
    }

    /**
     * 布局段浇
     * @param docAttr       文档属性
     * @param pageAttr      页面属性
     * @param paraAttr      段浇属性
     * @param para          布局段落视图
     * @param startOffset   布局开始Offset
     * @param x             布局开始x值
     * @param y             布局开始y值
     * @param w             布局的宽度
     * @param h             布局的高度
     * @param flag          布局标记
     * @return
     */
    public int layoutPara(IControl control, IDocument doc, DocAttr docAttr, PageAttr pageAttr, ParaAttr paraAttr,
        ParagraphView para, long startOffset, int x, int y, int w, int h, int flag)
    {
        // get paragraph token
        //ParaToken token = TokenManage.instance().allocToken(para);
        
        int breakType = WPViewConstant.BREAK_NO;
        int dx = paraAttr.leftIndent;
        int dy = 0;
        // 智能右边距预留：根据容器宽度调整策略
        int rightMarginReserve;
        if (w < 100) {
            // 很窄的容器（如表格单元格）：预留较少空间
            rightMarginReserve = Math.max(5, w / 10); // 最少5像素或10%宽度
        } else if (w < 300) {
            // 中等宽度容器：适中预留
            rightMarginReserve = Math.max(10, w / 15); // 最少10像素或6.7%宽度
        } else {
            // 宽容器：标准预留
            rightMarginReserve = Math.max(20, w / 20); // 最少20像素或5%宽度
        }
        
        int spanW = w - paraAttr.leftIndent - paraAttr.rightIndent - rightMarginReserve;
        spanW = spanW < 0 ? w - Math.min(rightMarginReserve, w / 2) : spanW; // 确保至少有50%宽度可用
        
        System.out.println("LayoutKit: Paragraph width calculation - totalW: " + w + 
                          ", leftIndent: " + paraAttr.leftIndent + 
                          ", rightIndent: " + paraAttr.rightIndent + 
                          ", rightMarginReserve: " + rightMarginReserve + 
                          ", finalSpanW: " + spanW + 
                          ", paragraphDx: " + dx);
        int spanH = h;
        int paraHeight = 0;
        int maxWidth = ViewKit.instance().getBitValue(flag, WPViewConstant.LAYOUT_NOT_WRAP_LINE) ? 0 : w;
        boolean firstLine = true;
        IElement elem = para.getElement();
        long lineStart = startOffset;
        long elemEnd = elem.getEndOffset();
        // 处理段前段后间距
        IView prePara = para.getPreView();
        if (prePara == null) // 页面第一个段落
        {
            spanH -= paraAttr.beforeSpace;
            para.setTopIndent(paraAttr.beforeSpace);
            para.setBottomIndent(paraAttr.afterSpace);
            para.setY(para.getY() + paraAttr.beforeSpace);
        }
        else
        {
            if (paraAttr.beforeSpace > 0)
            {
                int beforeSpace = paraAttr.beforeSpace - prePara.getBottomIndent();
                beforeSpace = Math.max(0, beforeSpace);
                spanH -= beforeSpace;
                para.setTopIndent(beforeSpace);
                para.setY(para.getY() + beforeSpace);
            }
            spanH -= paraAttr.afterSpace;
            para.setBottomIndent(paraAttr.afterSpace);
            
        }
        boolean keepOne = ViewKit.instance().getBitValue(flag, WPViewConstant.LAYOUT_FLAG_KEEPONE);
        
        // 如果spanH太小且不是keepOne，直接返回BREAK_LIMIT
        if (spanH < 20 && !keepOne)
        {
            System.out.println("LayoutKit: spanH too small (" + spanH + "), returning BREAK_LIMIT");
            para.setSize(0, 0);
            para.setEndOffset(startOffset);
            return WPViewConstant.BREAK_LIMIT;
        }
        LineView line = (LineView)ViewFactory.createView(control, elem, elem, WPViewConstant.LINE_VIEW);
        line.setStartOffset(lineStart);
        para.appendChlidView(line);       
        flag = ViewKit.instance().setBitValue(flag, WPViewConstant.LAYOUT_FLAG_KEEPONE, true);
        boolean ss = ViewKit.instance().getBitValue(flag, WPViewConstant.LAYOUT_FLAG_DELLELINEVIEW);
        BNView bnView = null;
        int bnViewWidth = -1;

        int paraLoopCount = 0; // 添加段落布局循环计数器
        final int MAX_PARA_LOOP_COUNT = 1000; // 最大段落布局循环次数
        
        while (spanH > 0 && lineStart < elemEnd && breakType != WPViewConstant.BREAK_PAGE && paraLoopCount < MAX_PARA_LOOP_COUNT)
        {
            paraLoopCount++; // 增加循环计数
            long oldLineStart = lineStart; // 保存旧的行开始位置
            
            // layout bullet and number
            if (firstLine && startOffset == elem.getStartOffset())
            {
                bnView = createBNView(control, doc, docAttr, pageAttr, paraAttr, para, dx, dy, spanW, spanH, flag);
                if (bnView != null)
                {
                    bnViewWidth = bnView.getWidth();
                }
            }
            int lineIndent = getLineIndent(control, bnViewWidth, paraAttr, firstLine);
            if (bnView != null && lineIndent + paraAttr.leftIndent == paraAttr.tabClearPosition)
            {
                if ((AttrManage.instance().hasAttribute(elem.getAttribute(), AttrIDConstant.PARA_SPECIALINDENT_ID)
                    && AttrManage.instance().getParaSpecialIndent(elem.getAttribute()) < 0)
                    || AttrManage.instance().hasAttribute(elem.getAttribute(), AttrIDConstant.PARA_INDENT_LEFT_ID))
                {
                    bnView.setX(0);
                    lineIndent = bnViewWidth;
                    dx = 0;
                }
            }
            line.setLeftIndent(lineIndent);
            line.setLocation(dx + lineIndent , dy);
            breakType = layoutLine(control, doc, docAttr, pageAttr, paraAttr, line, bnView, dx, dy, spanW - lineIndent, spanH, elemEnd, flag);
            int lineHeight = line.getLayoutSpan(WPViewConstant.Y_AXIS);
            // 严格的分页逻辑：确保内容不会超出页面边界
            boolean heightExceeded = spanH - lineHeight < 0; // 不允许任何超出
            boolean noChildView = line.getChildView() == null;
            boolean widthExceeded = spanW - lineIndent <= 0;
            
            // 额外检查：确保不会侵入页脚区域和左右页边距
            boolean wouldExceedPageBounds = false;
            boolean wouldExceedHorizontalBounds = false;
            
            if (pageAttr != null)
            {
                // 垂直边界检查（页脚）
                int currentY = dy + lineHeight;
                int maxAllowedY = pageAttr.pageHeight - pageAttr.bottomMargin;
                wouldExceedPageBounds = currentY > maxAllowedY;
                
                // 水平边界检查（左右页边距）- 使用相对坐标系统
                int lineWidth = line.getLayoutSpan(WPViewConstant.X_AXIS);
                int availableWidth = w; // 传入的可用宽度已经考虑了页边距
                
                // 简单检查：行宽是否超出可用宽度（已经考虑了页边距）
                wouldExceedHorizontalBounds = lineWidth > availableWidth;
                
                if (wouldExceedPageBounds)
                {
                    System.out.println("LayoutKit: Line would exceed vertical bounds - currentY: " + currentY + 
                                      ", maxAllowedY: " + maxAllowedY + 
                                      ", pageHeight: " + pageAttr.pageHeight + 
                                      ", bottomMargin: " + pageAttr.bottomMargin);
                }
                
                if (wouldExceedHorizontalBounds)
                {
                    System.out.println("LayoutKit: Line would exceed horizontal bounds - lineWidth: " + lineWidth + 
                                      ", availableWidth: " + availableWidth + 
                                      ", pageWidth: " + pageAttr.pageWidth + 
                                      ", leftMargin: " + pageAttr.leftMargin + 
                                      ", rightMargin: " + pageAttr.rightMargin);
                }
            }
            
            if (!ss && !keepOne && (heightExceeded || noChildView || widthExceeded || wouldExceedPageBounds || wouldExceedHorizontalBounds))
            {
                System.out.println("LayoutKit: Breaking line - heightExceeded: " + heightExceeded + 
                                  ", noChildView: " + noChildView + 
                                  ", widthExceeded: " + widthExceeded +
                                  ", wouldExceedPageBounds: " + wouldExceedPageBounds +
                                  ", wouldExceedHorizontalBounds: " + wouldExceedHorizontalBounds +
                                  ", spanH: " + spanH + ", lineHeight: " + lineHeight +
                                  ", spanW: " + spanW + ", lineIndent: " + lineIndent);
                breakType = WPViewConstant.BREAK_LIMIT;
                para.deleteView(line, true);
                break;
            }
            paraHeight += lineHeight;
            dy += lineHeight;
            spanH -= lineHeight;
            lineStart = line.getEndOffset(null);
            
            // 防止行开始位置没有前进导致的无限循环
            if (lineStart <= oldLineStart)
            {
                lineStart = oldLineStart + 1; // 强制前进至少一个位置
                if (lineStart >= elemEnd)
                {
                    break;
                }
            }
            
            maxWidth = Math.max(maxWidth, line.getLayoutSpan(WPViewConstant.X_AXIS));
            if (lineStart < elemEnd && spanH > 0)
            {
                line = (LineView)ViewFactory.createView(control, elem, elem, WPViewConstant.LINE_VIEW);
                line.setStartOffset(lineStart);
                para.appendChlidView(line);
            }
            keepOne = false;
            //flag = ViewKit.instance().setBitValue(flag, WPViewConstant.LAYOUT_FLAG_KEEPONE, keepOne);
            firstLine = false;
            bnView = null;
        }
        
        // 如果达到最大循环次数，记录警告
        if (paraLoopCount >= MAX_PARA_LOOP_COUNT)
        {
            System.err.println("Warning: Paragraph layout loop exceeded maximum count, breaking to prevent infinite loop");
        }
        para.setSize(maxWidth, paraHeight);
        para.setEndOffset(lineStart);
        
        // 修复：如果段落成功布局了内容，重置breakType为BREAK_NO
        // 这样页面就能继续布局更多段落
        if (para.getChildView() != null && lineStart >= elemEnd)
        {
            breakType = WPViewConstant.BREAK_NO;
            System.out.println("LayoutKit: Paragraph completed successfully, resetting breakType to BREAK_NO");
        }
        else if (para.getChildView() != null && breakType == WPViewConstant.BREAK_LIMIT)
        {
            // 如果段落有内容但没完成，也设为BREAK_NO让页面能继续尝试
            breakType = WPViewConstant.BREAK_NO;
            System.out.println("LayoutKit: Paragraph has content, resetting breakType to BREAK_NO");
        }
        
        //token.setFree(true);
        return breakType;
    }

    
    /**
     * 
     * @param doc
     * @param para
     * @param startOffset
     * @param x
     * @param y
     * @param w
     * @param h
     * @param flag
     * @return
     */
    public int buildLine(IDocument doc, ParagraphView para)
    {
        int breakType = WPViewConstant.BREAK_NO;
        //get paragraph token
        //ParaToken token = TokenManage.instance().allocToken(para);
        //
        /*AttrManage.instance().fillPageAttr(pageAttr, doc.getSection(0).getAttribute());
        //
        AttrManage.instance().fillParaAttr(paraAttr, para.getElement().getAttribute());
        
        int breakType = WPViewConstant.BREAK_NO;
        int dx = paraAttr.leftIndent;
        int dy = 0;
        int spanW = para.getWidth();
        int spanH = para.getHeight();
        int paraHeight = 0;
        int maxWidth = para.getWidth();
        boolean firstLine = true;
        IElement elem = para.getElement();
        long startOffset = para.getStartOffset(doc);
        long lineStart = startOffset;
        int flag = 0;
        long elemEnd = elem.getEndOffset();
        LineView line = (LineView)ViewFactory.createView(elem, elem, WPViewConstant.LINE_VIEW);
        line.setStartOffset(lineStart);
        para.appendChlidView(line);
        boolean keepOne = ViewKit.instance().getBitValue(flag, WPViewConstant.LAYOUT_FLAG_KEEPONE);
        flag = ViewKit.instance().setBitValue(flag, WPViewConstant.LAYOUT_FLAG_KEEPONE, true);
        boolean ss = ViewKit.instance().getBitValue(flag, WPViewConstant.LAYOUT_FLAG_DELLELINEVIEW);
        BNView bnView = null;
        while (spanH > 0 && lineStart < elemEnd)
        {
            // layout bullet and number
            if (firstLine && startOffset == elem.getStartOffset())
            {
                bnView = createBNView(doc, docAttr, pageAttr, paraAttr, para, dx, dy, spanW, spanH, flag);
            }
            int lineIndent = getLineIndent(bnView, paraAttr, firstLine);
            line.setLeftIndent(lineIndent);
            line.setLocation(dx + lineIndent , dy);
            breakType = layoutLine(doc, docAttr, pageAttr, paraAttr, line, bnView, dx, dy, spanW - lineIndent, spanH, elemEnd, flag);
            int lineHeight = line.getLayoutSpan(WPViewConstant.Y_AXIS);
            if (!ss && !keepOne
                && breakType == WPViewConstant.BREAK_LIMIT
                && (spanH - lineHeight < 0 || line.getChildView() == null)
                || spanW - lineIndent <= 0)
            {
                breakType = WPViewConstant.BREAK_LIMIT;
                para.deleteView(line, true);
                break;
            }
            paraHeight += lineHeight;
            dy += lineHeight;
            spanH -= lineHeight;
            lineStart = line.getEndOffset(null);
            maxWidth = Math.max(maxWidth, line.getLayoutSpan(WPViewConstant.X_AXIS));
            if (lineStart < elemEnd && spanH > 0)
            {
                line = (LineView)ViewFactory.createView(elem, elem, WPViewConstant.LINE_VIEW);
                line.setStartOffset(lineStart);
                para.appendChlidView(line);
            }
            keepOne = false;
            //flag = ViewKit.instance().setBitValue(flag, WPViewConstant.LAYOUT_FLAG_KEEPONE, keepOne);
            firstLine = false;
            bnView = null;
        }
        para.setSize(maxWidth, paraHeight);
        para.setEndOffset(lineStart);
        //
        //token.setFree(true);*/
        return breakType;
    }
    
    /**
     * 布局行
     * @param docAttr       文档属性       
     * @param pageAttr      页面属性
     * @param paraAttr      段落属性
     * @param line          布局的行
     * @param x             布局开始x值
     * @param y             布局开始y值
     * @param w             布局的宽度
     * @param h             布局的高度
     * @param maxEnd        布局的最大结束位置
     * @param flag          布局标记
     * @return
     */
    public int layoutLine(IControl control, IDocument doc, DocAttr docAttr, PageAttr pageAttr, ParaAttr paraAttr, LineView line,
        BNView bnView, int x, int y, int w, int h, long maxEnd, int flag)
    {
        System.out.println("LayoutKit: layoutLine start - w: " + w + ", h: " + h + ", maxEnd: " + maxEnd);
        int breakType = WPViewConstant.BREAK_NO;
        int dx = 0;
        int dy = 0;
        int spanW = w;
        int originalSpanW = w; // 保存原始行宽度，用于BREAK_LIMIT检查
        long start = line.getStartOffset(null);
        long pos = start;
        IElement elem = line.getElement();
        LeafView leaf = null;
        IElement run;
        int lineWidth = 0;
        int lineHeigth = 0;
        int lineHeigthExceptShape = 0;
        boolean keepOne = ViewKit.instance().getBitValue(flag, WPViewConstant.LAYOUT_FLAG_KEEPONE);
        int loopCount = 0; // 添加循环计数器防止无限循环
        final int MAX_LOOP_COUNT = 10000; // 最大循环次数
        
        while ((spanW > 0 && pos < maxEnd || keepOne) && loopCount < MAX_LOOP_COUNT)
        {
            loopCount++; // 增加循环计数
            
            run = doc.getLeaf(pos);
            if(run == null)
            {
            	break;
            }
            
            long oldPos = pos; // 保存旧位置
            
            leaf = (LeafView)ViewFactory.createView(control, run, elem, WPViewConstant.LEAF_VIEW);
            line.appendChlidView(leaf);
            leaf.setStartOffset(pos);
            leaf.setLocation(dx, dy);

            breakType = leaf.doLayout(docAttr, pageAttr, paraAttr, dx, dy, spanW, h, maxEnd, flag);
            
            // 严格处理BREAK_LIMIT：确保右边距和页面边界，防止文本超出
            if (breakType == WPViewConstant.BREAK_LIMIT && leaf.getType() == WPViewConstant.LEAF_VIEW)
            {
                int leafWidth = leaf.getLayoutSpan(WPViewConstant.X_AXIS);
                
                // 计算当前行的总宽度（包括已有内容）
                int currentLineWidth = dx + leafWidth;
                
                // 智能右边距预留：根据原始行宽度调整策略（不使用被修改的spanW）
                int rightMarginReserve;
                if (originalSpanW < 100) {
                    // 很窄的空间：预留较少
                    rightMarginReserve = Math.max(3, originalSpanW / 15); // 最少3像素或6.7%宽度
                } else if (originalSpanW < 300) {
                    // 中等宽度：适中预留
                    rightMarginReserve = Math.max(8, originalSpanW / 12); // 最少8像素或8.3%宽度
                } else {
                    // 宽空间：标准预留
                    rightMarginReserve = Math.max(15, originalSpanW / 10); // 最少15像素或10%宽度
                }
                int maxAllowedWidth = originalSpanW - rightMarginReserve;
                
                // 严格的换行判断：确保不超出右边距限制
                boolean fitsWithMargin = currentLineWidth <= maxAllowedWidth;
                boolean hasMinimalHeight = h > 20;
                
                // 只有在完全适合（包括右边距）且有足够高度时才忽略BREAK_LIMIT
                if (hasMinimalHeight && fitsWithMargin)
                {
                    System.out.println("LayoutKit: Ignoring BREAK_LIMIT - fits with all constraints (h:" + h + ", originalSpanW:" + originalSpanW + ", lineW:" + currentLineWidth + ", maxAllowed:" + maxAllowedWidth + ")");
                    breakType = WPViewConstant.BREAK_NO;
                }
                else
                {
                    System.out.println("LayoutKit: Keeping BREAK_LIMIT for line wrapping (h:" + h + ", originalSpanW:" + originalSpanW + ", lineW:" + currentLineWidth + ", maxAllowed:" + maxAllowedWidth + ", margin:" + rightMarginReserve + ")");
                }
            }
            
            if ((leaf.getType() == WPViewConstant.OBJ_VIEW || leaf.getType() == WPViewConstant.SHAPE_VIEW)
                && breakType == WPViewConstant.BREAK_LIMIT)
            {
                line.deleteView(leaf, true);
                breakType = WPViewConstant.BREAK_NO;
                break;
            }
            pos = leaf.getEndOffset(null);
            
            // 防止位置没有前进导致的无限循环
            if (pos <= oldPos)
            {
                pos = oldPos + 1; // 强制前进至少一个位置
                if (pos >= maxEnd)
                {
                    break;
                }
            }
            
            line.setEndOffset(pos);
            int leafWidth = leaf.getLayoutSpan(WPViewConstant.X_AXIS);
            lineWidth += leafWidth;
            dx += leafWidth;
            lineHeigth = Math.max(lineHeigth, leaf.getLayoutSpan(WPViewConstant.Y_AXIS));
            if(leaf.getType() != WPViewConstant.OBJ_VIEW && leaf.getType() != WPViewConstant.SHAPE_VIEW)
            {
            	lineHeigthExceptShape = Math.max(lineHeigthExceptShape, leaf.getLayoutSpan(WPViewConstant.Y_AXIS));
            }
            spanW -= leafWidth;
            if (breakType == WPViewConstant.BREAK_LIMIT
                || breakType == WPViewConstant.BREAK_ENTER
                || breakType == WPViewConstant.BREAK_PAGE)
            {
                break;
            }
            flag = ViewKit.instance().setBitValue(flag, WPViewConstant.LAYOUT_FLAG_KEEPONE, false);
            keepOne = false;
        }
        
        // 如果达到最大循环次数，记录警告
        if (loopCount >= MAX_LOOP_COUNT)
        {
            System.err.println("Warning: Layout loop exceeded maximum count, breaking to prevent infinite loop");
        }
        line.setSize(lineWidth, lineHeigth);
        line.setHeightExceptShape(lineHeigthExceptShape);
        
        System.out.println("LayoutKit: layoutLine end - childCount: " + 
                          (line.getChildView() != null ? "has children" : "no children") + 
                          ", breakType: " + breakType + ", lineWidth: " + lineWidth + ", lineHeight: " + lineHeigth);
        
        // 布局宽度受限，需要进行
        if (breakType == WPViewConstant.BREAK_LIMIT)
        {
            String str = elem.getText(doc);
            long paraStart = elem.getStartOffset();
            str = str.substring((int)(start - paraStart));
            long newPos = FontKit.instance().findBreakOffset(str, (int)(pos - start)) + start;
            adjustLine(line, newPos);
        }
        line.layoutAlignment(docAttr, pageAttr, paraAttr, bnView, w, flag);
        return breakType;
    }
    
    /**
     * 
     * @param doc
     * @param docAttr
     * @param pageAttr
     * @param paraAttr
     * @param para
     * @param startOffset
     * @param x
     * @param y
     * @param w
     * @param h
     * @param flag
     * @return
     */
    private BNView createBNView(IControl control, IDocument doc, DocAttr docAttr, PageAttr pageAttr, ParaAttr paraAttr,
        ParagraphView para, int x, int y, int w, int h, int flag)
    {
        if (paraAttr.listID >= 0 && paraAttr.listLevel >= 0
            || paraAttr.pgBulletID >= 0)
        {
            BNView bnView = (BNView)ViewFactory.createView(control, null, null, WPViewConstant.BN_VIEW);
            
            bnView.doLayout(doc, docAttr, pageAttr, paraAttr, para, x, y, w, h, flag);
            para.setBNView(bnView);
            
            return bnView;
        }
        return  null;
    }

    /**
     * 根据新的断行位置，调整视图
     * 
     * @param line
     * @param newPos
     */
    private void adjustLine(LineView line, long newPos)
    {
        IView view = line.getLastView();
        IView temp;
        int lineWidth = line.getWidth();
        while (view != null && view.getStartOffset(null) >= newPos)
        {
            temp = view.getPreView();
            lineWidth -= view.getWidth();
            line.deleteView(view, true);
            view = temp;
        }
        // 同一leaf，需要折分
        int leafWidth = 0;
        if (view != null && view.getEndOffset(null) > newPos)
        {
            view.setEndOffset(newPos);
            lineWidth -= view.getWidth();
            leafWidth = (int)((LeafView)view).getTextWidth();
            // 重置Leaf的宽度
            view.setWidth(leafWidth);
            lineWidth += leafWidth;
        }
        line.setEndOffset(newPos);
        line.setWidth(lineWidth);
    }
    
    /**
     * 
     */
    private int getLineIndent(IControl control, int bnViewWidth, ParaAttr paraAttr, boolean firstLine)
    {
        // 首先缩进
        if (firstLine)
        {
            int bnWidth = bnViewWidth <= 0 ? 0 : bnViewWidth;
            if (paraAttr.specialIndentValue > 0)
            {
                return paraAttr.specialIndentValue + bnWidth;
            }
            else
            {
                return bnWidth;
            }
            
        }
        // 悬挂缩进
        else if (!firstLine && paraAttr.specialIndentValue < 0)
        {
            if (bnViewWidth > 0 && control.getApplicationType() == MainConstant.APPLICATION_TYPE_PPT)
            {
                return bnViewWidth;
            }
            // 悬挂缩进 值也设置到左缩进，左缩进需要减去悬挂缩进            
            return -paraAttr.specialIndentValue;
        }
        return 0;
    }
    
    //
    //private DocAttr docAttr = new DocAttr();
    //
    //private PageAttr pageAttr = new PageAttr();
    //
    //private ParaAttr paraAttr = new ParaAttr(); 
}
