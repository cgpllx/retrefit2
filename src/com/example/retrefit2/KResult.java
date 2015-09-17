package com.example.retrefit2;

/**
 * 
 * @author Administrator
 *
 */
public interface KResult {
	/**
	 * 解析成功的标识
	 * 
	 * @return
	 */
	public boolean isSuccess();

	public String getFailureDesc();
}
