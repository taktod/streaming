package com.ttProject.xuggle;


/**
 * red5のストリームを変換するのを定義するマネージャー
 * red5のストリームとして、入力されたinputManagerのデータを出力としてoutputManagerに変換します。
 * @author taktod
 */
public class TranscodeManager {
	/** 入力ストリーム */
	private IInputManager inputManager;
	/** 出力ストリーム */
	private IOutputManager outputManager;
	public IInputManager getInputManager() {
		return inputManager;
	}
	public void setInputManager(IInputManager inputManager) {
		System.out.println("inputManager" + inputManager.hashCode());
		this.inputManager = inputManager;
	}
	public IOutputManager getOutputManager() {
		return outputManager;
	}
	public void setOutputManager(IOutputManager outputManager) {
		System.out.println("outputManager" + inputManager.hashCode());
		this.outputManager = outputManager;
	}
}
