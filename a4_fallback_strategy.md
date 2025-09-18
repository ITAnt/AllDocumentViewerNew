# A4纸张Fallback策略

## 🎯 策略目标
当文档的原始页面设置有问题或无法正常处理时，使用标准A4纸张设置作为fallback，确保文档始终有合理的显示效果。

## 📋 触发条件
以下任一条件满足时启用A4 fallback：
- `spanH <= 0` - 可用高度无效
- `spanW <= 0` - 可用宽度无效  
- `pageWidth < 200` - 页面宽度过小
- `pageHeight < 200` - 页面高度过小

## 📏 A4纸张标准规格

### 物理尺寸
- **A4纸张**: 210mm × 297mm
- **像素尺寸**: 794px × 1123px (96 DPI)
- **宽高比**: 1:1.414 (√2)

### 标准边距
- **边距**: 2.5cm = 71px (96 DPI)
- **左右边距**: 71px
- **上下边距**: 71px

## 🔧 实现逻辑

### 1. 基础A4设置
```java
int a4Width = 794;   // A4宽度
int a4Height = 1123; // A4高度
int standardMargin = 71; // 标准边距
```

### 2. 容器适应算法
```java
double a4Ratio = (double)a4Height / a4Width; // 1.414
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
```

### 3. 页面属性设置
```java
pageAttr.pageWidth = a4Width;
pageAttr.pageHeight = a4Height;
pageAttr.leftMargin = standardMargin;
pageAttr.rightMargin = standardMargin;
pageAttr.topMargin = standardMargin;
pageAttr.bottomMargin = standardMargin;
```

## 📊 优势特点

### 1. 标准化显示
- ✅ 使用国际标准A4纸张规格
- ✅ 保持专业文档外观
- ✅ 适合大多数文档类型

### 2. 自适应容器
- ✅ 保持A4比例不变形
- ✅ 适应不同屏幕尺寸
- ✅ 最大化利用可用空间

### 3. 合理边距
- ✅ 标准2.5cm边距，符合打印规范
- ✅ 确保内容不会贴边显示
- ✅ 提供良好的阅读体验

### 4. 兼容性保证
- ✅ 处理各种异常页面设置
- ✅ 确保文档始终可读
- ✅ 避免布局崩溃

## 🎯 应用场景

### 适用情况
- 损坏的DOCX文件页面设置
- 非标准页面尺寸文档
- 页面属性解析失败
- 极端页面尺寸（过大或过小）

### 预期效果
- 文档内容正常显示
- 保持良好的可读性
- 标准化的文档外观
- 稳定的布局表现

## 📝 日志输出
```
WPLayouter: Invalid dimensions - pageWidth: 66, pageHeight: 80, spanH: -112, spanW: -174, using A4 fallback
WPLayouter: Adjusted A4 size for container - width: 794, height: 1123
WPLayouter: Applied A4 fallback - pageWidth: 794, pageHeight: 1123, spanW: 652, spanH: 981, margins: 71px (standard A4)
```

这个A4 fallback策略确保了即使在文档页面设置有问题的情况下，用户也能获得标准、专业的文档显示效果。