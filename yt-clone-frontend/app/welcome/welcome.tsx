export function Welcome() {
  return (
    <main className="flex items-center justify-center pt-16 pb-4">
      <div className="flex-1 flex flex-col items-center gap-16 min-h-0">
        <div className="font-bold text-2xl">YT-clone</div>
        <div className="flex flex-wrap gap-3 ml-5">
          {videos.map(({ id, title, length, thumbnail }) => (
            <a href="#" key={id}>
              <div className="relative">
                {thumbnail}
                <span className="p-1 bg-black text-white rounded-xl absolute right-1 bottom-1">
                  {length}
                </span>
              </div>
              <div>{title}</div>
            </a>
          ))}
        </div>
      </div>
    </main>
  );
}

const videos = [
  {
    id: 1,
    title: "Test 1",
    length: "14:10",
    thumbnail: (
      <svg
        width="300"
        viewBox="0 0 512 512"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <g clip-path="url(#clip0_1766_3886)">
          <path d="M0 0H512V512H0V0Z" fill="#222D3A" />
          <path
            d="M338 195C368.928 195 394 169.928 394 139C394 108.072 368.928 83 338 83C307.072 83 282 108.072 282 139C282 169.928 307.072 195 338 195Z"
            fill="#C0C8CE"
          />
          <path
            d="M485.5 525L163.495 234.446C156.395 227.347 146.852 223.228 136.816 222.931C126.78 222.633 117.01 226.18 109.502 232.846L-214 525"
            fill="#B3BAC0"
          />
          <path
            opacity="0.7"
            d="M120.5 563.5L368.419 312.429C375.361 305.472 384.653 301.363 394.47 300.908C404.288 300.452 413.92 303.684 421.477 309.967L667 514"
            fill="#B3BAC0"
          />
        </g>
        <defs>
          <clipPath id="clip0_1766_3886">
            <rect width="512" height="512" fill="white" />
          </clipPath>
        </defs>
      </svg>
    ),
  },
  {
    id: 2,
    title: "Test 2",
    length: "1:36",
    thumbnail: (
      <svg
        width="300"
        viewBox="0 0 512 512"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <g clip-path="url(#clip0_1766_3886)">
          <path d="M0 0H512V512H0V0Z" fill="#222D3A" />
          <path
            d="M338 195C368.928 195 394 169.928 394 139C394 108.072 368.928 83 338 83C307.072 83 282 108.072 282 139C282 169.928 307.072 195 338 195Z"
            fill="#C0C8CE"
          />
          <path
            d="M485.5 525L163.495 234.446C156.395 227.347 146.852 223.228 136.816 222.931C126.78 222.633 117.01 226.18 109.502 232.846L-214 525"
            fill="#B3BAC0"
          />
          <path
            opacity="0.7"
            d="M120.5 563.5L368.419 312.429C375.361 305.472 384.653 301.363 394.47 300.908C404.288 300.452 413.92 303.684 421.477 309.967L667 514"
            fill="#B3BAC0"
          />
        </g>
        <defs>
          <clipPath id="clip0_1766_3886">
            <rect width="512" height="512" fill="white" />
          </clipPath>
        </defs>
      </svg>
    ),
  },
  {
    id: 3,
    title: "Test 3",
    length: "1:20:36",
    thumbnail: (
      <svg
        width="300"
        viewBox="0 0 512 512"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <g clip-path="url(#clip0_1766_3886)">
          <path d="M0 0H512V512H0V0Z" fill="#222D3A" />
          <path
            d="M338 195C368.928 195 394 169.928 394 139C394 108.072 368.928 83 338 83C307.072 83 282 108.072 282 139C282 169.928 307.072 195 338 195Z"
            fill="#C0C8CE"
          />
          <path
            d="M485.5 525L163.495 234.446C156.395 227.347 146.852 223.228 136.816 222.931C126.78 222.633 117.01 226.18 109.502 232.846L-214 525"
            fill="#B3BAC0"
          />
          <path
            opacity="0.7"
            d="M120.5 563.5L368.419 312.429C375.361 305.472 384.653 301.363 394.47 300.908C404.288 300.452 413.92 303.684 421.477 309.967L667 514"
            fill="#B3BAC0"
          />
        </g>
        <defs>
          <clipPath id="clip0_1766_3886">
            <rect width="512" height="512" fill="white" />
          </clipPath>
        </defs>
      </svg>
    ),
  },
  {
    id: 4,
    title: "Test 4",
    length: "0:34",
    thumbnail: (
      <svg
        width="300"
        viewBox="0 0 512 512"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <g clip-path="url(#clip0_1766_3886)">
          <path d="M0 0H512V512H0V0Z" fill="#222D3A" />
          <path
            d="M338 195C368.928 195 394 169.928 394 139C394 108.072 368.928 83 338 83C307.072 83 282 108.072 282 139C282 169.928 307.072 195 338 195Z"
            fill="#C0C8CE"
          />
          <path
            d="M485.5 525L163.495 234.446C156.395 227.347 146.852 223.228 136.816 222.931C126.78 222.633 117.01 226.18 109.502 232.846L-214 525"
            fill="#B3BAC0"
          />
          <path
            opacity="0.7"
            d="M120.5 563.5L368.419 312.429C375.361 305.472 384.653 301.363 394.47 300.908C404.288 300.452 413.92 303.684 421.477 309.967L667 514"
            fill="#B3BAC0"
          />
        </g>
        <defs>
          <clipPath id="clip0_1766_3886">
            <rect width="512" height="512" fill="white" />
          </clipPath>
        </defs>
      </svg>
    ),
  },
  {
    id: 5,
    title: "Test 5",
    length: "5:27",
    thumbnail: (
      <svg
        width="300"
        viewBox="0 0 512 512"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <g clip-path="url(#clip0_1766_3886)">
          <path d="M0 0H512V512H0V0Z" fill="#222D3A" />
          <path
            d="M338 195C368.928 195 394 169.928 394 139C394 108.072 368.928 83 338 83C307.072 83 282 108.072 282 139C282 169.928 307.072 195 338 195Z"
            fill="#C0C8CE"
          />
          <path
            d="M485.5 525L163.495 234.446C156.395 227.347 146.852 223.228 136.816 222.931C126.78 222.633 117.01 226.18 109.502 232.846L-214 525"
            fill="#B3BAC0"
          />
          <path
            opacity="0.7"
            d="M120.5 563.5L368.419 312.429C375.361 305.472 384.653 301.363 394.47 300.908C404.288 300.452 413.92 303.684 421.477 309.967L667 514"
            fill="#B3BAC0"
          />
        </g>
        <defs>
          <clipPath id="clip0_1766_3886">
            <rect width="512" height="512" fill="white" />
          </clipPath>
        </defs>
      </svg>
    ),
  },
];
