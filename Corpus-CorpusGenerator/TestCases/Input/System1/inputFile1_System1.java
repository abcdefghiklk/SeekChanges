package a;

import java.util.ArrayList;
import java.util.Hashtable;

enum Color
{
	WHITE, BLACK, RED, YELLOW, BLUE;
}

interface MyInterface
{
	void someMethod(int i, long l, int [] adf, int [][]adfe,long []wer[]);
	
	enum Color2
	{
		WHITE, BLACK, RED, YELLOW, BLUE;
	}
	
	class notPossible
	{
		
		/**
		 * comment for method m in inner class NotPossible
		 * second row 
		 */
		
		void m(String s,ArrayList<String> l,long a[][])
		{
			//comment method not possible m
			//second row
			//third row
			//4444 row
			//555 row
			//66 row
			//7 row
			//8row
			//999
			//10
			/* 1234
			 * ab
			 */
			int i=0;
			System.out.println(i);
		}
	}
	
	interface SubInterface
	{
		void methodSubInterface(String s2);
	}
}

abstract class A
{
	int i;

	/**
	 * @param j
	 * @param list
	 */
	abstract void ma1(int j,ArrayList<String> list);

	/**
	 * some method ma2
	 * 
	 * @param i
	 */
	void ma2(int i)
	{

	}

	void ma3()
	{
		//comments inside ma3
		int j=0;
		System.out.println(j);

	}

	/**
	 * @author B
	 *         this is a comment for class B
	 */
	class AB
	{
		int j;

		/**
		 * mab1 comment
		 */
		void mab1()
		{

		}

		/**
		 * @param j
		 *            comment mab2
		 */
		void mab2(float j)
		{

		}

		class ABC
		{
			/**
			 * @param s
			 */
			void mabc1(String s)
			{

			}

			/**
			 * @param j
			 */
			void mabc2(int j)
			{
				//mabc2

			}
		}

		class ABD
		{
			/**
			 * @param s
			 */
			void mabd1(String s)
			{

			}

			//some comment for i;
			int i;

			/**
			 * @param s
			 */
			void mabd2(String s)
			{

			}

			/**
			 * comment mabd3
			 * 
			 * @param j
			 */

			void mabd3(int j)
			{

			}
		}
	}

	/**
	 * @param l
	 */
	void ma4(long l)
	{

	}
}

class B
{
	int k;

	/**
	 * @param i
	 */
	void mb1(int i)
	{

	}

	/**
	 * @param s
	 */
	void mb2(ArrayList<String> s)
	{

	}

	class BE
	{
		/**
		 * @param ht
		 * @param b
		 */
		
		void mbe1(Hashtable<String,Integer> ht,int[] b)
		{

		}

		/**
		 * @param s
		 */
		void mbe2(String s)
		{

		}
	}

	/**
	 * comment mb3
	 */
	void mb3()
	{

	}
}

class C
{
	/**
	 * comment mc1
	 */
	void mc1()
	{
		//comment mc1
		int i;

	}

	/**
	 * @param i
	 * @param s
	 * @param f
	 * @param l
	 *            mc2 comment
	 */
	void mc2(int i,
			short s,
			float f,
			long l)
	{
		//comment inside mc2
	}
}

public class TestClass
{
	int i;

	/**
	 * @param args
	 */
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		// TODO Auto-generated method stub
	}
	
	//{{{ setDirty() method 
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@SuppressWarnings("unused")
	@Override
	public int hashCode()
	{
		// TODO Auto-generated method stub
		int i;
		return super.hashCode();
	} //}}}


	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		// TODO Auto-generated method stub
		return super.toString();
	}

	/**
	 * @author B
	 *         subclass InnerClass
	 */
	class InnerClass
	{
		/**
		 * @param parameter1
		 * @param parameter2
		 */
		void innerMethodNumberOne(int parameter1,float parameter2)
		{

		}
	}
}