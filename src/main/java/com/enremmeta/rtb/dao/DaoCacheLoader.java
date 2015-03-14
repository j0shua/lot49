package com.enremmeta.rtb.dao;

import java.util.concurrent.Future;

import com.google.common.cache.LoadingCache;

/**
 * Loader for {@link DaoCache}.
 * 
 * @see LoadingCache
 *
 * @author Gregory Golberg ( <a href="mailto:grisha@alum.mit.edu">grisha@alum.mit.edu</a>)
 *         <p>
 *         </p>
 *         Copyright © <a href="http://www.enremmeta.com">Enremmeta LLC</a> 2015. All Rights
 *         Reserved. 
 *
 */
public interface DaoCacheLoader<T> {
    Future<T> load(String key);
}
