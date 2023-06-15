/*
 *
 *  *     HMCLeaves
 *  *     Copyright (C) 2022  Hibiscus Creative Studios
 *  *
 *  *     This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License, or
 *  *     (at your option) any later version.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU General Public License
 *  *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package io.github.fisher2911.hmcleaves.util;

import java.util.Collection;
import java.util.SplittableRandom;

public class RandomUtil {

    public static final SplittableRandom RANDOM = new SplittableRandom();

    public static int randomInt(int min, int max) {
        return RANDOM.nextInt(min, max);
    }

    public static <E> E randomElement(Collection<E> collection) {
        return collection.stream().skip(randomInt(0, collection.size())).findFirst().orElse(null);
    }

}
