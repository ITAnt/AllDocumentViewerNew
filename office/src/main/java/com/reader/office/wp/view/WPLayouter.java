/*
 * 文件名称:          WPLayouter.java
 *  
 * 编译器:            android2.2
 * 时间:              下午2:13:54
 */
package com.reader.office.wp.view;

import java.util.ArrayList;
import java.util.List;

import com.reader.office.constant.wp.AttrIDConstant;
import com.reader.office.constant.wp.WPModelConstant;
import com.reader.office.constant.wp.WPViewConstant;
import com.reader.office.simpletext.model.AttrManage;
import com.reader.office.simpletext.model.IDocument;
import com.reader.office.simpletext.model.IElement;
import com.reader.office.simpletext.view.DocAttr;
import com.reader.office.simpletext.view.IView;
import com.reader.office.simpletext.view.PageAttr;
import com.reader.office.simpletext.view.ParaAttr;
import com.reader.office.simpletext.view.ViewKit;
import com.reader.office.wp.model.WPDocument;


/**
 * Word 布局器
 * <p>
 * <p>
 * Read版本:        Read V1.0
 * <p>
 * 作者:            ljj8494
 * <p>
 * 日期:            2011-11-20
 * <p>
 * 负责人:          ljj8494
 * <p>
 * 负责小组:         
 * <p>
 * <p>
 */
public class WPLayouter
{
    /**
     * 
     * @param root
     */
    public WPLayouter(PageRoot root)
    {
        this.root = root;
        docAttr = new DocAttr();
        docAttr.rootType = WPViewConstant.PAGE_ROOT;
        pageAttr = new PageAttr();
        paraAttr = new ParaAttr();
        tableLayout = new TableLayoutKit();
        hfTableLayout = new TableLayoutKit();
        
        // 初始化安全计数器
        layoutAttempts = 0;
    }
    
    /**
     * 
     */
    public void doLayout()
    {
        layoutAttempts++;
        System.out.println("WPLayouter: Starting layout process, attempt: " + layoutAttempts);
        
        // 全局安全检查：防止布局方法被反复调用
        if (layoutAttempts > 3) // 降低到3次，更快发现问题
        {
            System.err.println("WPLayouter: Too many layout attempts (" + layoutAttempts + "), stopping to prevent infinite recursion");
            
            // 强制完成布局
            if (doc != null)
            {
                long maxEnd = doc.getAreaEnd(WPModelConstant.MAIN);
                currentLayoutOffset = maxEnd;
                
                // 如果没有页面，创建一个空页面
                if (root.getChildView() == null)
                {
                    PageView emptyPage = (PageView)ViewFactory.createView(root.getControl(), section, null, WPViewConstant.PAGE_VIEW);
                    emptyPage.setPageNumber(1);
                    emptyPage.setStartOffset(0);
                    emptyPage.setEndOffset(maxEnd);
                    root.appendChlidView(emptyPage);
                }
            }
            return;
        }
        
        tableLayout.clearBreakPages();
        doc = root.getDocument();
        
        if (doc == null)
        {
            System.err.println("WPLayouter: Document is null, cannot layout");
            return;
        }
        
        // 正文区或
        // mainRange = doc.getRange();
        section = doc.getSection(0);
        
        if (section == null)
        {
            System.err.println("WPLayouter: No section found, cannot layout");
            return;
        }
        
        long mainAreaEnd = doc.getAreaEnd(WPModelConstant.MAIN);
        System.out.println("WPLayouter: Main area end: " + mainAreaEnd + ", current offset: " + currentLayoutOffset);
        
        // 
        AttrManage.instance().fillPageAttr(pageAttr, section.getAttribute());
        //
        PageView pv = (PageView)ViewFactory.createView(root.getControl(), section, null, WPViewConstant.PAGE_VIEW);
        root.appendChlidView(pv);
        
        System.out.println("WPLayouter: Starting page layout");
        layoutPage(pv);
        
        System.out.println("WPLayouter: Layout completed, finalizing");
        LayoutKit.instance().layoutAllPage(root, 1.0f);
    }
    
    /**
     * 
     */
    public int layoutPage(PageView pageView)
    {
    	System.out.println("WPLayouter: Layout page " + currentPageNumber + ", offset: " + currentLayoutOffset + ", isLayouting: " + isLayouting);
    	
    	// 防止重复布局
    	if (isLayouting)
    	{
    		System.err.println("WPLayouter: Already layouting, preventing recursive call");
    		return WPViewConstant.BREAK_LIMIT;
    	}
    	
    	isLayouting = true;
    	
    	try {
	    	// 防止页码无限增长
	    	if (currentPageNumber > 10) // 进一步降低到10页
	    	{
	    		System.err.println("Warning: Page number exceeded maximum limit (10), stopping layout");
	    		return WPViewConstant.BREAK_LIMIT;
	    	}
    	
    	pageView.setPageNumber(currentPageNumber++);
    	
        // 先设置基本的页面属性
        int breakType = WPViewConstant.BREAK_NO;
        pageView.setSize(pageAttr.pageWidth, pageAttr.pageHeight);
        pageView.setStartOffset(currentLayoutOffset);
        
        // 布局页眉页脚（这可能会修改边距）
        layoutHeaderAndFooter(pageView);
        
        // 重新设置页面缩进（使用可能被修改的边距）
        pageView.setIndent(pageAttr.leftMargin, pageAttr.topMargin, pageAttr.rightMargin, pageAttr.bottomMargin);
        
        // 恢复原始设置，但需要调试实际的坐标系统
        int dx = pageAttr.leftMargin;
        int dy = pageAttr.topMargin;
        int spanW = pageAttr.pageWidth - pageAttr.leftMargin - pageAttr.rightMargin;
        int spanH = pageAttr.pageHeight - pageAttr.topMargin - pageAttr.bottomMargin;
        
        System.out.println("WPLayouter: Content positioning - dx: " + dx + ", dy: " + dy + 
                          ", spanW: " + spanW + ", spanH: " + spanH + 
                          ", leftMargin: " + pageAttr.leftMargin + ", rightMargin: " + pageAttr.rightMargin);
        
        // 调试页面属性
        System.out.println("WPLayouter: Page attributes - width: " + pageAttr.pageWidth + 
                          ", height: " + pageAttr.pageHeight + 
                          ", topMargin: " + pageAttr.topMargin + 
                          ", bottomMargin: " + pageAttr.bottomMargin + 
                          ", leftMargin: " + pageAttr.leftMargin + 
                          ", rightMargin: " + pageAttr.rightMargin);
        
        // 当文档页面设置有问题时，使用标准A4纸张设置
        if (spanH <= 0 || spanW <= 0 || pageAttr.pageWidth < 200 || pageAttr.pageHeight < 200)
        {
            System.err.println("WPLayouter: Invalid dimensions - pageWidth: " + pageAttr.pageWidth + 
                              ", pageHeight: " + pageAttr.pageHeight + ", spanH: " + spanH + ", spanW: " + spanW + ", using A4 fallback");
            
            // 标准A4纸张尺寸（以像素为单位，96 DPI）
            // A4: 210mm x 297mm = 794px x 1123px (at 96 DPI)
            int a4Width = 794;
            int a4Height = 1123;
            
            // 尝试从root获取容器尺寸来调整A4比例
            try {
                if (root != null) {
                    int rootWidth = root.getLayoutSpan(WPViewConstant.X_AXIS);
                    int rootHeight = root.getLayoutSpan(WPViewConstant.Y_AXIS);
                    
                    if (rootWidth > 0 && rootHeight > 0) {
                        // 保持A4比例，但适应容器尺寸
                        double a4Ratio = (double)a4Height / a4Width; // 1.414 (A4比例)
                        double containerRatio = (double)rootHeight / rootWidth;
                        
                        if (containerRatio > a4Ratio) {
                            // 容器更高，以宽度为准
                            a4Width = Math.min(rootWidth, a4Width);
                            a4Height = (int)(a4Width * a4Ratio);
                        } else {
                            // 容器更宽，以高度为准
                            a4Height = Math.min(rootHeight, a4Height);
                            a4Width = (int)(a4Height / a4Ratio);
                        }
                        
                        System.out.println("WPLayouter: Adjusted A4 size for container - width: " + a4Width + ", height: " + a4Height);
                    }
                }
            } catch (Exception e) {
                System.err.println("WPLayouter: Could not get root size, using standard A4");
            }
            
            // 设置A4页面尺寸
            pageAttr.pageWidth = a4Width;
            pageAttr.pageHeight = a4Height;
            
            // 标准A4边距（约2.5cm = 71px at 96 DPI）
            int standardMargin = 71;
            pageAttr.leftMargin = standardMargin;
            pageAttr.rightMargin = standardMargin;
            pageAttr.topMargin = standardMargin;
            pageAttr.bottomMargin = standardMargin;
            
            // 重新计算
            spanW = pageAttr.pageWidth - pageAttr.leftMargin - pageAttr.rightMargin;
            spanH = pageAttr.pageHeight - pageAttr.topMargin - pageAttr.bottomMargin;
            
            // 更新页面视图的尺寸和边距
            pageView.setSize(pageAttr.pageWidth, pageAttr.pageHeight);
            pageView.setIndent(pageAttr.leftMargin, pageAttr.topMargin, pageAttr.rightMargin, pageAttr.bottomMargin);
            
            System.out.println("WPLayouter: Applied A4 fallback - pageWidth: " + pageAttr.pageWidth + 
                              ", pageHeight: " + pageAttr.pageHeight + 
                              ", spanW: " + spanW + ", spanH: " + spanH +
                              ", margins: " + standardMargin + "px (standard A4)");
        }
        
        int flag = ViewKit.instance().setBitValue(0, WPViewConstant.LAYOUT_FLAG_KEEPONE, true);
        long maxEnd = doc.getAreaEnd(WPModelConstant.MAIN);
        
        System.out.println("WPLayouter: Page layout - maxEnd: " + maxEnd + ", currentOffset: " + currentLayoutOffset + ", spanH: " + spanH);
        
        // 如果spanH太小，强制设置一个最小值以确保能渲染内容
        if (spanH <= 10)
        {
            System.err.println("WPLayouter: spanH too small (" + spanH + "), using minimum height");
            spanH = 100; // 强制使用100像素的最小高度
            System.out.println("WPLayouter: Forced spanH to: " + spanH);
        }
        
        // 安全检查：如果当前偏移已经超过或等于最大结束位置，直接返回
        if (currentLayoutOffset >= maxEnd)
        {
            System.out.println("WPLayouter: Current offset >= maxEnd, finishing layout");
            pageView.setEndOffset(currentLayoutOffset);
            return WPViewConstant.BREAK_NO;
        }
       
        IElement elem = breakPara != null ? breakPara.getElement() : doc.getParagraph(currentLayoutOffset);
        
        // 安全检查：如果没有找到段落元素
        if (elem == null)
        {
            System.err.println("WPLayouter: No paragraph element found at offset " + currentLayoutOffset);
            pageView.setEndOffset(currentLayoutOffset);
            return WPViewConstant.BREAK_NO;
        }
        
        ParagraphView para = null;
        if (breakPara != null)
        {
            para = breakPara;
            // process table break;
            if (breakPara.getType() == WPViewConstant.TABLE_VIEW)
            {
                pageView.setHasBreakTable(true);
                ((TableView)breakPara).setBreakPages(true);
            }
        }
        else if (AttrManage.instance().hasAttribute(elem.getAttribute(), AttrIDConstant.PARA_LEVEL_ID))
        {
            elem = ((WPDocument)doc).getParagraph0(currentLayoutOffset);
            para = (ParagraphView)ViewFactory.createView(root.getControl(), elem, null, WPViewConstant.TABLE_VIEW);
        }
        else
        {   
            para = (ParagraphView)ViewFactory.createView(root.getControl(), elem, null, WPViewConstant.PARAGRAPH_VIEW);
        }
        pageView.appendChlidView(para);
        
        para.setStartOffset(currentLayoutOffset);
        para.setEndOffset(elem.getEndOffset());
        boolean keepOne = true;
        int pageLoopCount = 0; // 添加页面布局循环计数器
        final int MAX_PAGE_LOOP_COUNT = 1000; // 最大页面布局循环次数
        
        // 修改循环条件：即使spanH很小也要尝试布局至少一些内容
        while (currentLayoutOffset < maxEnd && breakType != WPViewConstant.BREAK_LIMIT
            && breakType != WPViewConstant.BREAK_PAGE && pageLoopCount < MAX_PAGE_LOOP_COUNT
            && (spanH > 0 || keepOne)) // 允许keepOne时即使spanH<=0也进入循环
        {
            pageLoopCount++; // 增加循环计数
            long oldLayoutOffset = currentLayoutOffset; // 保存旧的布局偏移
            
            // 每次循环都输出调试信息（前几次）
            if (pageLoopCount <= 5)
            {
                System.out.println("WPLayouter: Page layout loop " + pageLoopCount + 
                                  ", offset: " + currentLayoutOffset + 
                                  ", spanH: " + spanH + 
                                  ", paraType: " + (para != null ? para.getType() : "null"));
            }
            
            para.setLocation(dx, dy);
            
            // 调试段落位置设置
            if (pageLoopCount <= 3) {
                System.out.println("WPLayouter: Set paragraph location - dx: " + dx + ", dy: " + dy + 
                                  ", paraX: " + para.getX() + ", paraY: " + para.getY());
            }
            
            // 表格段落
            if (para.getType() == WPViewConstant.TABLE_VIEW)
            {             
                if (para.getPreView() != null)
                {
                    if (para.getPreView().getElement() != elem)
                    {
                        tableLayout.clearBreakPages();
                    }
                }
                breakType = tableLayout.layoutTable(root.getControl(), doc, root, docAttr, pageAttr, paraAttr, 
                    (TableView)para, currentLayoutOffset, dx, dy, spanW, spanH, flag, breakPara != null);
            }
            else
            {
                tableLayout.clearBreakPages();
                AttrManage.instance().fillParaAttr(root.getControl(), paraAttr, elem.getAttribute());
                breakType = LayoutKit.instance().layoutPara(root.getControl(), doc, docAttr, pageAttr, paraAttr, 
                    para, currentLayoutOffset, dx, dy, spanW, spanH, flag);
            }
            int paraHeight = para.getLayoutSpan(WPViewConstant.Y_AXIS);
            
            // 详细调试段落布局结果
            if (pageLoopCount <= 5)
            {
                System.out.println("WPLayouter: Para layout result - height: " + paraHeight + 
                                  ", hasChild: " + (para.getChildView() != null) + 
                                  ", breakType: " + breakType +
                                  ", endOffset: " + para.getEndOffset(null));
            }
            
            if (!keepOne && para.getChildView() == null)
            {
                System.out.println("WPLayouter: Paragraph has no child views, deleting");
                if (breakPara == null)
                {
                    elem = doc.getParagraph(currentLayoutOffset - 1);
                }
                pageView.deleteView(para, true);
                break;
            }
            //
            if (para.getType() != WPViewConstant.TABLE_VIEW)
            {
                root.getViewContainer().add(para);
            }
            // 收集段落中的 shape view
            collectShapeView(pageView, para, false);
            
            dy += paraHeight;
            long newLayoutOffset = para.getEndOffset(null);
            
            // 调试偏移量变化
            if (pageLoopCount <= 5)
            {
                System.out.println("WPLayouter: Offset change: " + currentLayoutOffset + " -> " + newLayoutOffset);
            }
            
            currentLayoutOffset = newLayoutOffset;
            
            // 防止布局偏移没有前进导致的无限循环
            if (currentLayoutOffset <= oldLayoutOffset)
            {
                System.out.println("WPLayouter: Offset not advancing (" + oldLayoutOffset + " -> " + currentLayoutOffset + "), forcing page break");
                currentLayoutOffset = oldLayoutOffset + 1; // 强制前进至少一个位置
                if (currentLayoutOffset >= maxEnd)
                {
                    break;
                }
                // 强制结束当前页面布局，避免无限循环
                break;
            }
            
            spanH -= paraHeight;
            
            // 严格的页面边界检查：确保内容不会超出页眉页脚和左右页边距
            // 预留足够的空间给下一个段落，防止内容溢出到页脚区域
            int minRequiredSpace = 50; // 至少需要50像素空间才能放置下一个段落
            boolean hasEnoughSpace = spanH > minRequiredSpace;
            boolean withinVerticalBounds = (dy + minRequiredSpace) <= (pageAttr.pageHeight - pageAttr.bottomMargin);
            
            // 水平边界检查：简化检查，主要依赖段落布局中的宽度控制
            boolean withinHorizontalBounds = true;
            if (para != null)
            {
                int paraWidth = para.getLayoutSpan(WPViewConstant.X_AXIS);
                // 简单检查：段落宽度是否超出可用宽度
                withinHorizontalBounds = paraWidth <= spanW;
                
                if (!withinHorizontalBounds)
                {
                    System.out.println("WPLayouter: Paragraph width exceeds available space - paraWidth: " + paraWidth + 
                                      ", spanW: " + spanW);
                }
            }
            
            System.out.println("WPLayouter: Boundary check - spanH: " + spanH + 
                              ", dy: " + dy + 
                              ", pageHeight: " + pageAttr.pageHeight + 
                              ", bottomMargin: " + pageAttr.bottomMargin + 
                              ", hasEnoughSpace: " + hasEnoughSpace + 
                              ", withinVerticalBounds: " + withinVerticalBounds +
                              ", withinHorizontalBounds: " + withinHorizontalBounds);
            
            // 只有在段落正常完成(BREAK_NO)且严格满足所有边界条件时才继续布局
            if (hasEnoughSpace && withinVerticalBounds && withinHorizontalBounds && currentLayoutOffset < maxEnd && breakType == WPViewConstant.BREAK_NO)
            {                
                elem = doc.getParagraph(currentLayoutOffset);
                if (elem == null) // 添加空检查
                {
                    break;
                }
                if (AttrManage.instance().hasAttribute(elem.getAttribute(), AttrIDConstant.PARA_LEVEL_ID))
                {
                    if (elem != para.getElement())
                    {
                        tableLayout.clearBreakPages(); 
                    }
                    elem = ((WPDocument)doc).getParagraph0(currentLayoutOffset);
                    if (elem == null) // 添加空检查
                    {
                        break;
                    }
                    para = (ParagraphView)ViewFactory.createView(root.getControl(), elem, null, WPViewConstant.TABLE_VIEW);                    
                }
                else
                {
                    para = (ParagraphView)ViewFactory.createView(root.getControl(), elem, null, WPViewConstant.PARAGRAPH_VIEW);
                }
                para.setStartOffset(currentLayoutOffset);
                pageView.appendChlidView(para);
            }
            flag = ViewKit.instance().setBitValue(flag, WPViewConstant.LAYOUT_FLAG_KEEPONE, false);
            breakPara = null;
            keepOne = false;
        }
        
        // 如果达到最大循环次数，记录警告并强制结束
        if (pageLoopCount >= MAX_PAGE_LOOP_COUNT)
        {
            System.err.println("Warning: Page layout loop exceeded maximum count, forcing layout completion");
            
            // 强制设置结束偏移量
            if (currentLayoutOffset < maxEnd)
            {
                currentLayoutOffset = maxEnd;
            }
            
            // 确保页面有结束偏移量
            pageView.setEndOffset(currentLayoutOffset);
            
            // 返回限制标志，停止进一步的布局
            return WPViewConstant.BREAK_LIMIT;
        }
        // table
        if (para.getType() == WPViewConstant.TABLE_VIEW && tableLayout.isTableBreakPages())
        {
            breakPara = (ParagraphView)ViewFactory.createView(root.getControl(), elem, null, WPViewConstant.TABLE_VIEW);
            pageView.setHasBreakTable(true);
            ((TableView)para).setBreakPages(true);
        }
        // 
        else if (elem != null && currentLayoutOffset < elem.getEndOffset())
        {
            breakPara = (ParagraphView)ViewFactory.createView(root.getControl(), elem, null, WPViewConstant.PARAGRAPH_VIEW);
        }
        
        pageView.setEndOffset(currentLayoutOffset);
        //
        root.getViewContainer().sort();
        //
        root.addPageView(pageView);
        //
        pageView.setPageBackgroundColor(pageAttr.pageBRColor);
        //
        pageView.setPageBorder(pageAttr.pageBorder);
        
        return breakType;
        
    	} finally {
    		isLayouting = false;
    	}
    }
    
    
    /**
     * 
     */
    private void layoutHeaderAndFooter(PageView pageView)
    {
        if (header == null)
        {
            header = layoutHFParagraph(pageView, true);
            if (header != null)
            {
                int h = header.getLayoutSpan(WPViewConstant.Y_AXIS);
                int newTopMargin = pageAttr.headerMargin + h;
                
                // 限制页眉不能占用超过页面高度的1/3
                int maxTopMargin = pageAttr.pageHeight / 3;
                if (newTopMargin > pageAttr.topMargin && newTopMargin <= maxTopMargin)
                {
                    pageAttr.topMargin = newTopMargin;
                }
                else if (newTopMargin > maxTopMargin)
                {
                    System.err.println("WPLayouter: Header too large, limiting top margin to " + maxTopMargin);
                    pageAttr.topMargin = maxTopMargin;
                }
                header.setParentView(pageView);
            }
        }
        else
        {
            for (LeafView sv :shapeViews)
            {                
                if (WPViewKit.instance().getArea(sv.getStartOffset(null)) == WPModelConstant.HEADER)
                {
                    pageView.addShapeView(sv);
                }
            }
        }
        pageView.setHeader(header);
        if (footer == null)
        {
            footer = layoutHFParagraph(pageView, false);
            if (footer != null)
            {                
                int newBottomMargin = pageAttr.pageHeight - footer.getY();
                
                // 限制页脚不能占用超过页面高度的1/3
                int maxBottomMargin = pageAttr.pageHeight / 3;
                if (newBottomMargin > pageAttr.bottomMargin && newBottomMargin <= maxBottomMargin)
                {
                    pageAttr.bottomMargin = newBottomMargin;
                }
                else if (newBottomMargin > maxBottomMargin)
                {
                    System.err.println("WPLayouter: Footer too large, limiting bottom margin to " + maxBottomMargin);
                    pageAttr.bottomMargin = maxBottomMargin;
                }
                footer.setParentView(pageView);
            }
        }
        else
        {
            for (LeafView sv :shapeViews)
            {                
                if (WPViewKit.instance().getArea(sv.getStartOffset(null)) == WPModelConstant.FOOTER)
                {
                    pageView.addShapeView(sv);
                }
            }
        }
        
        pageView.setFooter(footer);
    }
    
    /**
     * 
     */
    private TitleView layoutHFParagraph(PageView pageView, boolean isHeader)
    {
        long offset = isHeader ? WPModelConstant.HEADER : WPModelConstant.FOOTER;
        int breakType = WPViewConstant.BREAK_NO;
        IElement hfElem = doc.getHFElement(offset, WPModelConstant.HF_ODD);
        if (hfElem == null)
        {
            return null;
        }
        
        //ignore line pitch for header and footer layout
    	float oldLinePitch = pageAttr.pageLinePitch;
    	pageAttr.pageLinePitch = -1;
    	
        TitleView titleView = (TitleView)ViewFactory.createView(root.getControl(), hfElem, null, WPViewConstant.TITLE_VIEW);
        titleView.setPageRoot(root);
        titleView.setLocation(pageAttr.leftMargin, pageAttr.headerMargin);
        
        long maxEnd = hfElem.getEndOffset();
        int spanW = pageAttr.pageWidth - pageAttr.leftMargin - pageAttr.rightMargin;
        int spanH = (pageAttr.pageHeight - pageAttr.topMargin - pageAttr.bottomMargin - 100) / 2;
        int flag = ViewKit.instance().setBitValue(0, WPViewConstant.LAYOUT_FLAG_KEEPONE, true);
        ParagraphView para = null;
        IElement paraElem = doc.getParagraph(offset);
        if (AttrManage.instance().hasAttribute(paraElem.getAttribute(), AttrIDConstant.PARA_LEVEL_ID))
        {
            paraElem = ((WPDocument)doc).getParagraph0(offset);
            para = (ParagraphView)ViewFactory.createView(root.getControl(), paraElem, null, WPViewConstant.TABLE_VIEW);
        }
        else
        {   
            para = (ParagraphView)ViewFactory.createView(root.getControl(), paraElem, null, WPViewConstant.PARAGRAPH_VIEW);
        }        
        titleView.appendChlidView(para);
        
        para.setStartOffset(offset);
        para.setEndOffset(paraElem.getEndOffset());
        boolean keepOne = true;
        int dx = 0;
        int dy = 0;
        int titleHeight = 0;
        while (spanH > 0 && offset < maxEnd && breakType != WPViewConstant.BREAK_LIMIT)
        {
            para.setLocation(dx, dy);
            // 表格段落
            if (para.getType() == WPViewConstant.TABLE_VIEW)
            {
                breakType = hfTableLayout.layoutTable(root.getControl(), doc, root, docAttr, pageAttr, paraAttr, 
                    (TableView)para, offset, dx, dy, spanW, spanH, flag, breakPara != null);
            }
            else
            {
                hfTableLayout.clearBreakPages();
                AttrManage.instance().fillParaAttr(root.getControl(), paraAttr, paraElem.getAttribute());
                breakType = LayoutKit.instance().layoutPara(root.getControl(), doc, docAttr, pageAttr, paraAttr, 
                    para, offset, dx, dy, spanW, spanH, flag);
            }
            int paraHeight = para.getLayoutSpan(WPViewConstant.Y_AXIS);
            if (!keepOne && para.getChildView() == null)
            {
                titleView.deleteView(para, true);
                break;
            }
            dy += paraHeight;
            titleHeight += paraHeight;
            offset = para.getEndOffset(null);
            spanH -= paraHeight;
            // 收集段落中的 shape view
            collectShapeView(pageView, para, true);
            if (spanH > 0 && offset < maxEnd && breakType != WPViewConstant.BREAK_LIMIT)
            {
                paraElem = doc.getParagraph(offset);
                if (AttrManage.instance().hasAttribute(paraElem.getAttribute(), AttrIDConstant.PARA_LEVEL_ID))
                {
                    paraElem = ((WPDocument)doc).getParagraph0(offset);
                    para = (ParagraphView)ViewFactory.createView(root.getControl(), paraElem, null, WPViewConstant.TABLE_VIEW);
                }
                else
                {
                    para = (ParagraphView)ViewFactory.createView(root.getControl(), paraElem, null, WPViewConstant.PARAGRAPH_VIEW);
                }
                para.setStartOffset(offset);
                titleView.appendChlidView(para);
            }
            flag = ViewKit.instance().setBitValue(flag, WPViewConstant.LAYOUT_FLAG_KEEPONE, false);
            keepOne = false;
        }
        titleView.setSize(spanW, titleHeight);
        if (!isHeader)
        {
            titleView.setY(pageAttr.pageHeight - titleHeight - pageAttr.footerMargin);
        }
        
        //restore line pitch
    	pageAttr.pageLinePitch = oldLinePitch;
    	
        return titleView;
    }
    
    /**
     * 
     */
    public void backLayout()
    {
        PageView pv = (PageView)ViewFactory.createView(root.getControl(), section, null, WPViewConstant.PAGE_VIEW);
        root.appendChlidView(pv);
        layoutPage(pv);
    }

    /**
     * @return Returns the currentLayoutOffset.
     */
    public long getCurrentLayoutOffset()
    {
        return currentLayoutOffset;
    }

    /**
     * @param currentLayoutOffset The currentLayoutOffset to set.
     */
    public void setCurrentLayoutOffset(long currentLayoutOffset)
    {
        this.currentLayoutOffset = currentLayoutOffset;
    }
    
    /**
     * 
     */
    public boolean isLayoutFinish()
    {
        return currentLayoutOffset >= doc.getAreaEnd(WPModelConstant.MAIN) && breakPara == null;
    }
    
    /**
     * 
     */
    private void collectShapeView(PageView page, ParagraphView para, boolean isHF)
    {
        if (para.getType() == WPViewConstant.PARAGRAPH_VIEW)
        {
            collectShapeViewForPara(page, para, isHF);
        }
        else if (para.getType() == WPViewConstant.TABLE_VIEW)
        {
            IView row = para.getChildView();
            while (row != null)
            {
                IView cell = row.getChildView();
                while (cell != null)
                {
                    IView paraView = cell.getChildView();
                    while (paraView != null)
                    {
                        collectShapeViewForPara(page, (ParagraphView)para, isHF);
                        paraView = paraView.getNextView();
                    }
                    cell = cell.getNextView();
                }
                row = row.getNextView();
            }
        }
    }
    
    /**
     * 
     */
    private void collectShapeViewForPara(PageView page, ParagraphView para, boolean isHF)
    {
        IView line = para.getChildView();
        while (line != null)
        {
            IView leaf = line.getChildView();
            while (leaf != null)
            {                    
                if (leaf.getType() == WPViewConstant.SHAPE_VIEW)
                {
                    ShapeView shapeView = ((ShapeView)leaf);
                    if (!shapeView.isInline())
                    {
                        page.addShapeView(shapeView);
                        if (isHF)
                        {
                            shapeViews.add(shapeView);
                        }
                    }                        
                }
                else if(leaf.getType() == WPViewConstant.OBJ_VIEW)
                {
                    ObjView objView = ((ObjView)leaf);
                    if (!objView.isInline())
                    {
                        page.addShapeView(objView);
                        if (isHF)
                        {
                            shapeViews.add(objView);
                        }
                    } 
                }
                leaf = leaf.getNextView();
            }
            line = line.getNextView();
        }
    }
    
    /**
     * 
     */
    public void dispose()
    {
        docAttr.dispose();
        docAttr = null;
        pageAttr.dispose();
        pageAttr = null;
        paraAttr.dispose();
        paraAttr = null;
        root = null;
        doc = null;
        breakPara = null;
        header = null;
        footer = null;
        tableLayout = null;
        hfTableLayout = null;
        shapeViews.clear();
    }
    

    // 文档属性集
    private DocAttr docAttr;
    // 章节属性集
    private PageAttr pageAttr;
    // 段落
    private ParaAttr paraAttr; 
    //
    private PageRoot root;
    //
    private IDocument doc;
    
    
    // ======== 布局过程用到一些布局状态的值 ==========
    private IElement section;
    // 当前布局的页码数
    private int currentPageNumber = 1;
    // 当前需要布局的开始的Offset，主要为了段落切页用到。
    private long currentLayoutOffset;
    // 段落分页
    private ParagraphView breakPara;
    // header
    private TitleView header;
    // footer
    private TitleView footer;
    //
    private TableLayoutKit tableLayout;
    //
    private TableLayoutKit hfTableLayout;
    //
    private List<LeafView> shapeViews = new ArrayList<LeafView>();
    
    // 布局尝试计数器，防止无限递归
    private int layoutAttempts;
    
    // 布局状态标志，防止重复布局
    private boolean isLayouting = false;
    
}
