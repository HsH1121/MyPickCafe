/*
 * 비밀번호 입력 안내 — 서버 PasswordPolicy 의 길이 규칙을 입력 중에 미리 알려 준다.
 * 최종 판단(흔한 비밀번호 차단 등)은 서버가 한다. 여기서는 길이만 본다.
 *
 * 사용: <input type="password" data-password-policy> 와
 *       <p data-password-policy-hint="입력 id"> (선택), <input data-password-confirm="새 비밀번호 입력 id"> (선택)
 *
 * 기준(서버와 동일 — PasswordPolicyFrontendSyncTest 가 숫자를 맞춰 본다)
 *   - 글자 수: 유니코드 코드 포인트 기준 8자 이상 20자 이하 (Array.from(value).length)
 *     input 의 maxlength 는 이모지를 2칸(UTF-16)으로 세서 서버와 어긋나므로 쓰지 않고 여기서 막는다.
 *   - UTF-8 72바이트 이하 (BCrypt 한도. 한글 20자 = 60바이트라 통과, 이모지 같은 4바이트 문자만 쓰면 18자까지)
 *   - 공백은 허용하며 잘라내지 않는다. 공백만으로는 안 된다.
 *   - 제어 문자(C0·DEL·C1, 줄/문단 구분자)는 안 된다.
 */
(function () {
  var MIN = 8, MAX = 20, MAX_BYTES = 72;

  function utf8Bytes(s) {
    return new TextEncoder().encode(s).length;
  }

  function check(value, optional) {
    if (!value) return optional ? '' : '비밀번호를 입력해주세요.';
    if (!value.trim()) return '공백만으로는 비밀번호를 만들 수 없습니다.';
    var len = Array.from(value).length;
    if (len < MIN) return '비밀번호는 ' + MIN + '자 이상이어야 합니다. (현재 ' + len + '자)';
    if (len > MAX) return '비밀번호는 ' + MAX + '자 이하여야 합니다.';
    if (utf8Bytes(value) > MAX_BYTES) return '비밀번호가 ' + MAX_BYTES + '바이트를 넘었습니다.';
    if (/[\u0000-\u001f\u007f-\u009f\u2028\u2029]/.test(value)) return '줄바꿈·탭 같은 제어 문자는 쓸 수 없습니다.';
    return '';
  }

  function bind(input) {
    var optional = input.hasAttribute('data-password-optional');
    var hint = document.querySelector('[data-password-policy-hint="' + input.id + '"]');
    var defaultHint = hint ? hint.textContent : '';
    function update() {
      var msg = check(input.value, optional);
      input.setCustomValidity(msg);
      if (hint) {
        hint.textContent = msg || defaultHint;
        hint.style.color = msg && input.value ? 'var(--danger)' : '';
      }
    }
    input.addEventListener('input', update);
    update();
  }

  function bindConfirm(confirm) {
    var target = document.getElementById(confirm.getAttribute('data-password-confirm'));
    if (!target) return;
    function update() {
      confirm.setCustomValidity(confirm.value && confirm.value !== target.value ? '비밀번호가 일치하지 않습니다.' : '');
    }
    confirm.addEventListener('input', update);
    target.addEventListener('input', update);
  }

  document.querySelectorAll('input[data-password-policy]').forEach(bind);
  document.querySelectorAll('input[data-password-confirm]').forEach(bindConfirm);
})();
