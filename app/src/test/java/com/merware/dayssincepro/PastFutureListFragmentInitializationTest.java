package com.merware.dayssincepro;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PastFutureListFragmentInitializationTest {

    @Test
    public void listData_beforeFragmentInitialization_isDeferred() {
        PastFutureListFragment fragment = new PastFutureListFragment();

        fragment.listData();
    }

    @Test
    public void listDataAjax_beforeFragmentInitialization_isDeferred() {
        PastFutureListFragment fragment = new PastFutureListFragment();

        fragment.listDataAjax("query");

        assertEquals("query", fragment.searchText);
    }
}
