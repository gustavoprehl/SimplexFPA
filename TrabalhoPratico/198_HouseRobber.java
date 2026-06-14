class Solution {
    public int rob(int[] nums) {
        int prev = 0, curr = 0;
        for (int num : nums) {
            int next = Math.max(curr, prev + num);
            prev = curr;
            curr = next;
        }
        return curr;
    }
}
