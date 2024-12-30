
package com.yx.pass.remote.pms.model.resp.servers.page;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

@Data
@AllArgsConstructor
public class CustomPage<T> implements IPage<T> {
    private static final long serialVersionUID = 8545996863226528798L;
    protected List<T> records;
    protected long total;
    protected long size;
    protected long current;
    protected List<OrderItem> orders;
    protected boolean optimizeCountSql;
    protected boolean searchCount;
    protected boolean optimizeJoinOfCountSql;
    protected Long maxLimit;
    protected String countId;
    protected Integer totalServer;
    protected List<Integer> cards;

    public CustomPage() {
        this.records = Collections.emptyList();
        this.total = 0L;
        this.size = 10L;
        this.current = 1L;
        this.orders = new ArrayList();
        this.optimizeCountSql = true;
        this.searchCount = true;
        this.optimizeJoinOfCountSql = true;
    }

    public CustomPage(long current, long size) {
        this(current, size, 0L);
    }

    public CustomPage(long current, long size, long total) {
        this(current, size, total, true);
    }

    public CustomPage(long current, long size, boolean searchCount) {
        this(current, size, 0L, searchCount);
    }

    public CustomPage(long current, long size, long total, boolean searchCount) {
        this.records = Collections.emptyList();
        this.total = 0L;
        this.size = 10L;
        this.current = 1L;
        this.orders = new ArrayList();
        this.optimizeCountSql = true;
        this.searchCount = true;
        this.optimizeJoinOfCountSql = true;
        if (current > 1L) {
            this.current = current;
        }

        this.size = size;
        this.total = total;
        this.searchCount = searchCount;
    }

    public boolean hasPrevious() {
        return this.current > 1L;
    }

    public boolean hasNext() {
        return this.current < this.getPages();
    }

    public List<T> getRecords() {
        return this.records;
    }

    public CustomPage<T> setRecords(List<T> records) {
        this.records = records;
        return this;
    }

    public long getTotal() {
        return this.total;
    }

    public CustomPage<T> setTotal(long total) {
        this.total = total;
        return this;
    }

    public long getSize() {
        return this.size;
    }

    public CustomPage<T> setSize(long size) {
        this.size = size;
        return this;
    }

    public long getCurrent() {
        return this.current;
    }

    public CustomPage<T> setCurrent(long current) {
        this.current = current;
        return this;
    }

    public String countId() {
        return this.countId;
    }

    public Long maxLimit() {
        return this.maxLimit;
    }

    private String[] mapOrderToArray(Predicate<OrderItem> filter) {
        List<String> columns = new ArrayList(this.orders.size());
        this.orders.forEach((i) -> {
            if (filter.test(i)) {
                columns.add(i.getColumn());
            }

        });
        return (String[])columns.toArray(new String[0]);
    }

    private void removeOrder(Predicate<OrderItem> filter) {
        for(int i = this.orders.size() - 1; i >= 0; --i) {
            if (filter.test(this.orders.get(i))) {
                this.orders.remove(i);
            }
        }

    }

    public CustomPage<T> addOrder(OrderItem... items) {
        this.orders.addAll(Arrays.asList(items));
        return this;
    }

    public CustomPage<T> addOrder(List<OrderItem> items) {
        this.orders.addAll(items);
        return this;
    }

    public List<OrderItem> orders() {
        return this.orders;
    }

    public boolean optimizeCountSql() {
        return this.optimizeCountSql;
    }

    public static <T> CustomPage<T> of(long current, long size, long total, boolean searchCount) {
        return new CustomPage(current, size, total, searchCount);
    }

    public boolean optimizeJoinOfCountSql() {
        return this.optimizeJoinOfCountSql;
    }

    public CustomPage<T> setSearchCount(boolean searchCount) {
        this.searchCount = searchCount;
        return this;
    }

    public CustomPage<T> setOptimizeCountSql(boolean optimizeCountSql) {
        this.optimizeCountSql = optimizeCountSql;
        return this;
    }

    public long getPages() {
        return IPage.super.getPages();
    }

    public static <T> CustomPage<T> of(long current, long size) {
        return of(current, size, 0L);
    }

    public static <T> CustomPage<T> of(long current, long size, long total) {
        return of(current, size, total, true);
    }

    public static <T> CustomPage<T> of(long current, long size, boolean searchCount) {
        return of(current, size, 0L, searchCount);
    }

    public boolean searchCount() {
        return this.total < 0L ? false : this.searchCount;
    }

    /** @deprecated */
    @Deprecated
    public String getCountId() {
        return this.countId;
    }

    /** @deprecated */
    @Deprecated
    public Long getMaxLimit() {
        return this.maxLimit;
    }

    /** @deprecated */
    @Deprecated
    public List<OrderItem> getOrders() {
        return this.orders;
    }

    /** @deprecated */
    @Deprecated
    public boolean isOptimizeCountSql() {
        return this.optimizeCountSql;
    }

    /** @deprecated */
    @Deprecated
    public boolean isSearchCount() {
        return this.searchCount;
    }

    public void setOrders(final List<OrderItem> orders) {
        this.orders = orders;
    }

    public void setOptimizeJoinOfCountSql(final boolean optimizeJoinOfCountSql) {
        this.optimizeJoinOfCountSql = optimizeJoinOfCountSql;
    }

    public void setMaxLimit(final Long maxLimit) {
        this.maxLimit = maxLimit;
    }

    public void setCountId(final String countId) {
        this.countId = countId;
    }
}
