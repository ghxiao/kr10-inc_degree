package edu.pku.id;

import java.util.ArrayList;
import java.util.List;

import org.sat4j.core.VecInt;
import org.sat4j.specs.IVecInt;

public class QCTranslater extends MultiValuedTranslater {

	// encoding method
	// y_p -> 4p - 3
	// y_{\neg p} -> 4p -2
	// +p -> 4p - 1
	// -p -> 4p
	// z_i -> 4 * nVars + i
	
	int clauseIndex = 0;

	// @Override
	// public List<IVecInt> getWeightedClauses() {
	// List<IVecInt> clausesOut = new ArrayList<IVecInt>();
	// for(IVecInt clause:clauses){
	// clausesOut.addAll(translateClause(clause));
	// }
	// return clausesOut;
	// }

	/**
	 * 灏嗕竴涓瓙鍙ヨ繘琛屽己鍙樻崲<br/>
	 * 绠楁硶濡備笅:<br/>
	 * S(l_1\vee \ldots \vee l_n) = \vee_{i=1}^n(+l_i\wedge \neg - l_i) \vee
	 * \wedge_{i=1}^n(+l_i\wedge -l_i)<br/>
	 * 寮曞叆鏂板彉閲�鏉ラ伩鍏嶅彉鎹㈠悗鐨勯暱搴﹀彂鐢熸寚鏁扮垎鐐�br/> y_i <- +l_i\wedge \neg - l_i, i = 1,
	 * \ldots, n<br/>
	 * z <- \wedge_{i=1}^n(+l_i\wedge -l_i)<br/>
	 * 杩欐牱鍦ㄤ繚鎸佸彲婊¤冻鎬т笅,鍙互鍙樻崲涓�br/> (\vee_{i=1}^n y_i \vee z) \wedge
	 * \wedge_{i=1}^n(\neg y_i \vee +l_i) \wedge \wedge_{i=1}^n(\neg y_i \vee
	 * \neg -l_i) \wedge_{i=1}^n(\neg z \vee +l_i) \wedge \wedge_{i=1}^n(\neg z
	 * \vee - l_i)
	 * 
	 * 淇濊瘉鍙弧瓒虫�鐨勫彉鎹腑寮曞叆鐨�y_i = +l_i \wedge \neg - l_i鐨勭紪鐮佷负4x+2 寮曞叆鐨剒 =
	 * \wedge_{i=1}^n (+l_i \wedge -l_i) 鐨勭紪鐮佷负3
	 * 
	 * @param clause
	 *            瑕佸彉鎹㈢殑瀛愬彞
	 * @return 寮哄彉鎹㈢殑缁撴灉
	 */
	private List<IVecInt> translateClause(IVecInt clause) {
		clauseIndex++;

		int n = clause.size();
		List<IVecInt> result = new ArrayList<IVecInt>(n * 4 + 1);

		IVecInt v;

		// (\vee_{i=1}^n y_i \vee z)
		v = new VecInt(n + 1);
		for (int i = 0; i < n; i++) {
			v.push(y(clause.get(i)));
		}
		v.push(z(clauseIndex));
		result.add(v);

		for (int i = 0; i < n; i++) {
			// l_i
			final int li = clause.get(i);

			// \neg y_i \vee +l_i
			v = new VecInt(2);
			v.push(-y(li));
			v.push(literalPositiveTransform(li));
			result.add(v);

			// \neg y_i \vee \neg -l_i
			v = new VecInt(2);
			v.push(-y(li));
			v.push(-literalNegativeTransform(li));
			result.add(v);

			// \neg z \vee +l_i
			v = new VecInt(2);
			v.push(-z(clauseIndex));
			v.push(literalPositiveTransform(li));
			result.add(v);

			// \neg z \vee -l_i
			v = new VecInt(2);
			v.push(-z(clauseIndex));
			v.push(literalNegativeTransform(li));
			result.add(v);
		}

		return result;
	}

	private int z(int index) {
		return nVars * 4 + index;
	}

	private int y(int literal) {
		int symbol = Math.abs(literal);
		if (literal > 0) {
			return 4 * symbol - 3;
		} else {// if(literal < 0){
			return 4 * symbol - 2;
		}

	}

	/**
	 * 姝ｅ師瀛恆鐨勭紪鐮佸鏋滄槸x<br/>
	 * +a鐨勭紪鐮佷负4x<br/>
	 * -a鐨勭紪鐮佷负4x+1<br/>
	 * 
	 * l -> +l<br/>
	 * a -> +a<br/>
	 * \neg a -> -a<br/>
	 */
	int literalPositiveTransform(int literal) {
		int symbol = Math.abs(literal);
		if (literal < 0) {
			// return 4 * symbol + 1;
			return 4 * symbol;
		} else {
			return 4 * symbol - 1;
		}
	}

	/**
	 * l -> -l a -> -a \neg a -> +a
	 */
	int literalNegativeTransform(int literal) {
		int symbol = Math.abs(literal);
		if (literal < 0) {
			return 4 * symbol - 1;
		} else {
			// return 4 * symbol + 1;
			return 4 * symbol;
		}
	}

	@Override
	void translate() {
		countVars();
		this.weightedClauses = new ArrayList<WeightedClause>(nVars + clauses.size());
		for (IVecInt clauseIn : clauses) {
			List<IVecInt> clausesOut = translateClause(clauseIn);
			for (IVecInt clauseOut : clausesOut)
				weightedClauses.add(new WeightedClause(nVars + 1, clauseOut));
		}

		for (int var = 1; var <= nVars; var++) {
			weightedClauses.add(new WeightedClause(1, new VecInt(new int[] { -translateLiteral(var), -translateLiteral(-var) })));
		}
	}

}
