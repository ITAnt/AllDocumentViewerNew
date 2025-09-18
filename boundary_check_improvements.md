# 完整页面边界检查改进

## 🎯 问题描述
文字内容超出页眉页脚区域和左右页边距，即使换页也无法正确限制内容在主体区域内。

## ✅ 实施的改进

### 1. WPLayouter 中的严格边界检查
- **垂直边界**:
  - `spanH > minRequiredSpace` (50像素)
  - `(dy + minRequiredSpace) <= (pageHeight - bottomMargin)` - 确保不会侵入页脚区域
- **水平边界**:
  - `paraX >= leftMargin` - 确保不会超出左页边距
  - `paraEndX <= (pageWidth - rightMargin)` - 确保不会超出右页边距
  - 添加段落宽度和位置的详细检查

### 2. LayoutKit 中的严格行边界检查
- **垂直检查**:
  - `spanH - lineHeight < 0` - 不允许任何高度超出
  - `currentY > maxAllowedY` - 检查是否会超出页面绝对边界
- **水平检查**:
  - `currentX < minAllowedX` - 检查是否超出左边界
  - `lineEndX > maxAllowedX` - 检查是否超出右边界
  - 考虑行的实际宽度和位置

### 3. BREAK_LIMIT 处理中的绝对边界检查
- **相对边界**: 基于spanW的右边距预留
- **绝对边界**: 基于pageAttr的绝对页面边界检查
- **双重验证**: 同时满足相对和绝对边界条件才允许继续

### 4. 增强的调试信息
- 垂直和水平边界检查的详细日志
- 显示当前位置与最大/最小允许位置的比较
- 显示页面尺寸和所有边距信息

## 🔧 技术细节

### 垂直边界计算公式
```java
int maxAllowedY = pageAttr.pageHeight - pageAttr.bottomMargin;
boolean withinVerticalBounds = (dy + minRequiredSpace) <= maxAllowedY;
```

### 水平边界计算公式
```java
int maxAllowedX = pageAttr.pageWidth - pageAttr.rightMargin;
int minAllowedX = pageAttr.leftMargin;
boolean withinHorizontalBounds = (currentX >= minAllowedX) && (lineEndX <= maxAllowedX);
```

### 绝对页面边界检查
```java
int absoluteX = x + dx + leafWidth;
boolean withinAbsolutePageBounds = (x + dx >= minAbsoluteX) && (absoluteX <= maxAbsoluteX);
```

### 检查条件层次
1. **基础空间检查**: 确保有足够的剩余空间
2. **相对边界检查**: 基于可用宽度的边距检查
3. **绝对边界检查**: 基于页面实际尺寸的边界检查
4. **边距保护**: 预留空间防止侵入页眉页脚和左右边距

## 📊 预期效果
- 文字内容严格限制在页面主体区域内
- 不会侵入页眉、页脚和左右页边距区域
- 更准确的分页和换行处理
- 更好的文档显示质量和专业外观
- 支持复杂布局（表格、多列等）的边界控制

## 🔍 检查覆盖范围
- ✅ 垂直边界（页眉/页脚）
- ✅ 水平边界（左右页边距）
- ✅ 段落级别检查
- ✅ 行级别检查
- ✅ 字符级别检查（BREAK_LIMIT）
- ✅ 表格内容检查
- ✅ 绝对和相对边界双重验证

这些改进确保了文档内容在所有情况下都严格保持在正确的页面边界内，提供完美的文档显示效果。