/* *******************************************************************
 * Copyright (c) 2002-2008 Contributors
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *     PARC     initial implementation 
 *     Andy Clement 
 * ******************************************************************/

package org.aspectj.weaver;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.aspectj.testing.util.TestUtil;

/**
 * An abstract set of tests that any World implementation should be able to pass. To run it against your World, subclass it and
 * implement getWorld().
 * 
 * @author Andy Clement
 */
public abstract class CommonWorldTests extends TestCase {

	/**
	 * @return an instance of the World to be tested
	 */
	protected abstract World getWorld();

	private World world;

	public void setUp() {
		world = getWorld();
	}

	private final UnresolvedType[] primitiveTypeXs = UnresolvedType.forSignatures(new String[] { "B", "S", "C", "I", "J", "F", "D",
			"V" });

	public void testPrimitiveTypes() {
		ResolvedType[] primitives = world.resolve(primitiveTypeXs);
		for (int i = 0, len = primitives.length; i < len; i++) {
			ResolvedType ty = primitives[i];
			modifiersTest(ty, Modifier.PUBLIC | Modifier.FINAL);
			fieldsTest(ty, ResolvedMember.NONE);
			methodsTest(ty, ResolvedMember.NONE);
			interfacesTest(ty, ResolvedType.NONE);
			superclassTest(ty, null);
			pointcutsTest(ty, ResolvedMember.NONE);
			isInterfaceTest(ty, false);
			isClassTest(ty, false);
			isAspectTest(ty, false);
			for (int j = 0; j < len; j++) {
				ResolvedType ty1 = primitives[j];
				if (ty.equals(ty1)) {
					isCoerceableFromTest(ty, ty1, true);
				} else if (ty == ResolvedType.BOOLEAN || ty1 == ResolvedType.BOOLEAN || ty == ResolvedType.VOID
						|| ty1 == ResolvedType.VOID) {
					isCoerceableFromTest(ty, ty1, false);
				} else {
					isCoerceableFromTest(ty, ty1, true);
				}
			}

			// Result of this depends on whether autoboxing is supported
			// isCoerceableFromTest(ty, UnresolvedType.OBJECT, getSupportsAutoboxing());

			primAssignTest("B", new String[] {});
			primAssignTest("S", new String[] { "B" });
			primAssignTest("C", new String[] { "B" });
			primAssignTest("I", new String[] { "B", "S", "C" });
			primAssignTest("J", new String[] { "B", "S", "C", "I" });
			primAssignTest("F", new String[] { "B", "S", "C", "I", "J" });
			primAssignTest("D", new String[] { "B", "S", "C", "I", "J", "F" });
			primAssignTest("Z", new String[] {});
			primAssignTest("V", new String[] {});

		}
	}

	private void primAssignTest(String sig, String[] lowers) {
		ResolvedType[] primitives = getWorld().resolve(primitiveTypeXs);
		UnresolvedType tx = UnresolvedType.forSignature(sig);
		ResolvedType ty = getWorld().resolve(tx, true);
		assertTrue("Couldnt find type " + tx, !ty.isMissing());
		ResolvedType[] lowerTyArray = getWorld().resolve(UnresolvedType.forSignatures(lowers));
		List lowerTys = new ArrayList(Arrays.asList(lowerTyArray));
		lowerTys.add(ty);
		Set allLowerTys = new HashSet(lowerTys);
		Set allUpperTys = new HashSet(Arrays.asList(primitives));
		allUpperTys.removeAll(allLowerTys);

		for (Iterator i = allLowerTys.iterator(); i.hasNext();) {
			ResolvedType other = (ResolvedType) i.next();
			isAssignableFromTest(ty, other, true);
		}
		for (Iterator i = allUpperTys.iterator(); i.hasNext();) {
			ResolvedType other = (ResolvedType) i.next();
			isAssignableFromTest(ty, other, false);
		}
	}

	public void testPrimitiveArrays() {
		ResolvedType[] primitives = world.resolve(primitiveTypeXs);
		for (int i = 0, len = primitives.length; i < len; i++) {
			ResolvedType ty = primitives[i];
			UnresolvedType tx = UnresolvedType.forSignature("[" + ty.getSignature());
			ResolvedType aty = getWorld().resolve(tx, true);
			assertTrue("Couldnt find type " + tx, !aty.isMissing());
			modifiersTest(aty, Modifier.PUBLIC | Modifier.FINAL);
			fieldsTest(aty, ResolvedMember.NONE);
			methodsTest(aty, ResolvedMember.NONE);
			interfaceTest(aty, new ResolvedType[] { world.getCoreType(UnresolvedType.CLONEABLE),
					world.getCoreType(UnresolvedType.SERIALIZABLE) });
			superclassTest(aty, UnresolvedType.OBJECT);

			pointcutsTest(aty, ResolvedMember.NONE);
			isInterfaceTest(aty, false);
			isClassTest(aty, false);
			isAspectTest(aty, false);
			for (int j = 0; j < len; j++) {
				ResolvedType ty1 = primitives[j];
				isCoerceableFromTest(aty, ty1, false);
				tx = UnresolvedType.forSignature("[" + ty1.getSignature());
				ResolvedType aty1 = getWorld().resolve(tx, true);
				assertTrue("Couldnt find type " + tx, !aty1.isMissing());
				if (ty.equals(ty1)) {
					isCoerceableFromTest(aty, aty1, true);
					isAssignableFromTest(aty, aty1, true);
				} else {
					isCoerceableFromTest(aty, aty1, false);
					isAssignableFromTest(aty, aty1, false);
				}
			}
		}
	}

	// ---- tests for parts of ResolvedType objects

	protected void modifiersTest(ResolvedType ty, int mods) {
		assertEquals(ty + " modifiers:", Modifier.toString(mods), Modifier.toString(ty.getModifiers()));
	}

	protected void fieldsTest(ResolvedType ty, Member[] x) {
		TestUtil.assertSetEquals(ty + " fields:", x, ty.getDeclaredJavaFields());
	}

	protected void methodsTest(ResolvedType ty, Member[] x) {
		TestUtil.assertSetEquals(ty + " methods:", x, ty.getDeclaredJavaMethods());
	}

	protected void mungersTest(ResolvedType ty, ShadowMunger[] x) {
		TestUtil.assertSetEquals(ty + " mungers:", x, ty.getDeclaredShadowMungersArray());
	}

	protected void interfaceTest(ResolvedType type, ResolvedType[] expectedInterfaces) {
		ResolvedType[] interfaces = type.getDeclaredInterfaces();
		for (int i = 0; i < expectedInterfaces.length; i++) {
			boolean wasMissing = true;
			for (int j = 0; j < interfaces.length; j++) {
				if (interfaces[j].getSignature().equals(expectedInterfaces[i].getSignature())) {
					wasMissing = false;
				}
			}
			if (wasMissing) {
				fail("Expected declared interface " + expectedInterfaces[i] + " but it wasn't found in "
						+ Arrays.asList(interfaces));
			}
		}
	}

	protected void interfacesTest(ResolvedType ty, ResolvedType[] x) {
		TestUtil.assertArrayEquals(ty + " interfaces:", x, ty.getDeclaredInterfaces());
	}

	protected void superclassTest(ResolvedType ty, UnresolvedType x) {
		assertEquals(ty + " superclass:", x, ty.getSuperclass());
	}

	protected void pointcutsTest(ResolvedType ty, Member[] x) {
		TestUtil.assertSetEquals(ty + " pointcuts:", x, ty.getDeclaredPointcuts());
	}

	protected void isInterfaceTest(ResolvedType ty, boolean x) {
		assertEquals(ty + " is interface:", x, ty.isInterface());
	}

	protected void isAspectTest(ResolvedType ty, boolean x) {
		assertEquals(ty + " is aspect:", x, ty.isAspect());
	}

	protected void isClassTest(ResolvedType ty, boolean x) {
		assertEquals(ty + " is class:", x, ty.isClass());
	}

	protected void isCoerceableFromTest(UnresolvedType ty0, UnresolvedType ty1, boolean x) {
		assertEquals(ty0 + " is coerceable from " + ty1, x, ty0.resolve(world).isCoerceableFrom(ty1.resolve(world)));
		assertEquals(ty1 + " is coerceable from " + ty0, x, ty1.resolve(world).isCoerceableFrom(ty0.resolve(world)));
	}

	protected void isAssignableFromTest(UnresolvedType ty0, UnresolvedType ty1, boolean x) {
		assertEquals(ty0 + " is assignable from " + ty1, x, ty0.resolve(world).isAssignableFrom(ty1.resolve(world)));
	}

	// ---- tests for parts of ResolvedMethod objects

	protected void modifiersTest(ResolvedMember m, int mods) {
		assertEquals(m + " modifiers:", Modifier.toString(mods), Modifier.toString(m.getModifiers()));
	}

	protected void exceptionsTest(ResolvedMember m, UnresolvedType[] exns) {
		TestUtil.assertSetEquals(m + " exceptions:", exns, m.getExceptions());
	}

}